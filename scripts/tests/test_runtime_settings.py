import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch, MagicMock

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import runtime_settings as runtime


class RuntimeSettingsTest(unittest.TestCase):
    def settings(self):
        return {"MAIL_USERNAME": "test@example.invalid", "MAIL_PASSWORD": "dummy ' $value ; & !\nnext",
                "KAKAO_ADMIN_KEY": "dummy-admin-value"}

    def test_missing_settings_report_names_only(self):
        with self.assertRaises(RuntimeError) as caught:
            runtime.validate_settings({"MAIL_USERNAME": "private-address@example.invalid"})
        self.assertIn("MAIL_PASSWORD", str(caught.exception))
        self.assertIn("KAKAO_ADMIN_KEY", str(caught.exception))
        self.assertNotIn("private-address", str(caught.exception))

    def test_environment_quotes_round_trip_without_shell_expansion(self):
        bash = os.getenv("BASH_EXECUTABLE") or shutil.which("bash")
        if not bash:
            self.skipTest("bash required for shell quoting test")
        settings = self.settings()
        code = runtime.render_environment(settings) + "printf '%s\\0' \"$MAIL_USERNAME\" \"$MAIL_PASSWORD\" \"$KAKAO_ADMIN_KEY\""
        result = subprocess.run([bash, "-c", code], capture_output=True, check=True)
        self.assertEqual(result.stdout.split(b"\0")[:-1], [settings[key].encode() for key in runtime.KEYS])

    def test_hook_overrides_old_exports_immediately_before_java(self):
        old = "#!/bin/bash\nexport MAIL_USERNAME=old\nexport JWT_SECRET=unchanged\nnohup java -jar /home/ec2-user/app.jar > server.log 2>&1 &\n"
        patched = runtime.patch_launcher(old, "/home/ec2-user/.i_route_runtime.env")
        self.assertLess(patched.index("export JWT_SECRET"), patched.index(runtime.BEGIN))
        self.assertLess(patched.index(runtime.END), patched.index("nohup java"))
        self.assertEqual(runtime.patch_launcher(patched, "/home/ec2-user/.i_route_runtime.env"), patched)
        self.assertEqual(patched.count(runtime.BEGIN), 1)

    def test_unknown_launcher_fails_before_modification(self):
        for script in ("#!/bin/bash\nrun-app\n", "java -version\nnohup java -jar app.jar &\n"):
            with self.subTest(script=script), self.assertRaises(RuntimeError):
                runtime.patch_launcher(script, "/tmp/test.env")

    @patch.object(runtime.smtplib, "SMTP")
    def test_mail_verification_authenticates_without_sending(self, smtp):
        runtime.verify_mail(self.settings())
        session = smtp.return_value.__enter__.return_value
        session.starttls.assert_called_once()
        session.login.assert_called_once_with(self.settings()["MAIL_USERNAME"], self.settings()["MAIL_PASSWORD"])
        session.sendmail.assert_not_called()
        session.send_message.assert_not_called()

    def prepare(self, directory):
        home = Path(directory)
        stage = home / ".deploy" / "test-run"
        stage.mkdir(parents=True)
        (home / "start.sh").write_text("#!/bin/bash\nexport DB_URL=keep\nnohup java -jar app.jar &\n")
        (home / "app.jar").write_bytes(b"previous-app")
        (stage / "app.jar").write_bytes(b"next-app")
        (stage / "runtime-settings.json").write_text(json.dumps(self.settings()))
        return home, stage

    @patch.object(runtime, "verify_mail")
    @patch.object(runtime, "restart")
    @patch.object(runtime, "wait_until_ready", return_value=True)
    @patch.object(runtime.subprocess, "run", return_value=MagicMock(returncode=0))
    def test_deploy_keeps_other_settings_and_validates_runtime(self, shell, ready, restart, mail):
        with tempfile.TemporaryDirectory(dir=Path(__file__).resolve().parents[2]) as directory:
            home, stage = self.prepare(directory)
            runtime.deploy(stage)
            self.assertEqual((home / "app.jar").read_bytes(), b"next-app")
            self.assertIn("export DB_URL=keep", (home / "start.sh").read_text())
            self.assertEqual((home / ".i_route_runtime.env").read_text(), runtime.render_environment(self.settings()))
            self.assertFalse((stage / "runtime-settings.json").exists())
            ready.assert_called_once_with(self.settings())
            restart.assert_called_once()

    @patch.object(runtime, "verify_mail")
    @patch.object(runtime, "restart")
    @patch.object(runtime, "wait_until_ready", side_effect=[False, True])
    @patch.object(runtime.subprocess, "run", return_value=MagicMock(returncode=0))
    def test_failed_deploy_restores_jar_launcher_and_environment(self, shell, ready, restart, mail):
        with tempfile.TemporaryDirectory(dir=Path(__file__).resolve().parents[2]) as directory:
            home, stage = self.prepare(directory)
            original = (home / "start.sh").read_text()
            with self.assertRaisesRegex(RuntimeError, "restored and healthy"):
                runtime.deploy(stage)
            self.assertEqual((home / "start.sh").read_text(), original)
            self.assertEqual((home / "app.jar").read_bytes(), b"previous-app")
            self.assertFalse((home / ".i_route_runtime.env").exists())
            self.assertEqual(restart.call_count, 2)


if __name__ == "__main__":
    unittest.main()
