package com.i_route.backend.message;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {
    @Query("""
            select m from DirectMessage m
            where ((m.senderId = :me and m.recipientId = :peer)
                or (m.senderId = :peer and m.recipientId = :me))
                and m.id < :before
            order by m.id desc
            """)
    List<DirectMessage> history(@Param("me") Long me, @Param("peer") Long peer,
                                @Param("before") Long before, Pageable page);

    @Query("""
            select m from DirectMessage m
            where (m.senderId = :me or m.recipientId = :me)
              and not exists (select newer.id from DirectMessage newer
                  where newer.id > m.id
                    and ((newer.senderId = m.senderId and newer.recipientId = m.recipientId)
                      or (newer.senderId = m.recipientId and newer.recipientId = m.senderId)))
            order by m.id desc
            """)
    List<DirectMessage> latestConversations(@Param("me") Long me);

    interface UnreadCount {
        Long getSenderId();
        long getTotal();
    }

    @Query("""
            select m.senderId as senderId, count(m) as total from DirectMessage m
            where m.recipientId = :me and m.readAt is null group by m.senderId
            """)
    List<UnreadCount> unreadBySender(@Param("me") Long me);

    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    @Modifying
    @Query("""
            update DirectMessage m set m.readAt = :now
            where m.senderId = :peer and m.recipientId = :me
              and m.id <= :through and m.readAt is null
            """)
    void markRead(@Param("me") Long me, @Param("peer") Long peer,
                  @Param("through") Long through, @Param("now") Instant now);
}
