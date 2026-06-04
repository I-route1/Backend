#!/bin/bash
export JWT_SECRET=aXJvdXRlLXN1cGVyLXNlY3JldC1rZXktZm9yLWp3dC10b2tlbi1zaWduaW5n
export I_ROUTE_DB_USERNAME=iroute
export I_ROUTE_DB_PASSWORD=iroute_pw
export DB_URL="jdbc:mysql://localhost:3306/i_route_db?serverTimezone=Asia/Seoul&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true"
export MAIL_USERNAME=dev@example.com
export MAIL_PASSWORD=dev-password
export KAKAO_CLIENT_ID=3eb63a2338882943f3b2280a3460d9c8
export KAKAO_REST_API_KEY=3eb63a2338882943f3b2280a3460d9c8
export KAKAO_REDIRECT_URI=http://54.180.118.100:8080/api/auth/kakao/callback
export AI_SERVER_URL=http://localhost:8082
export SPRING_PROFILES_ACTIVE=prod
nohup java -Dfile.encoding=UTF-8 -jar /home/ec2-user/app.jar > /home/ec2-user/server.log 2>&1 &
echo "?? ?? PID:$!"