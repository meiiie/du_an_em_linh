package vn.hoctapcanman.core.tutor.application.port;

import java.util.*;
import vn.hoctapcanman.core.tutor.application.dto.TutorSessionDto.*;

/** Giao dịch ngắn: không giữ khóa trong lúc gọi model hoặc cổng toán. */
public interface TutorSessionStore {
    Optional<Assignment> assignment(UUID student, UUID assignment);
    Optional<Assignment> assignmentByCode(UUID student, String code);
    Session open(Assignment assignment);
    Optional<Session> find(UUID student, UUID session);
    Session snapshot(UUID session);
    Accepted accept(Session session, Ask request, String question, String contextVersion);
    List<Turn> turns(UUID session, int offset, int limit);
    default List<Turn> memory(UUID session,long after,UUID exclude) { return turns(session,0,1000).stream().filter(t -> t.ordinal()>after && !t.id().equals(exclude) && !t.status().equals("generating")).sorted(java.util.Comparator.comparingLong(Turn::ordinal)).toList(); }
    default Optional<Turn> turn(UUID session, UUID turn) { return turns(session,0,1000).stream().filter(t -> t.id().equals(turn)).findFirst(); }
    default History history(UUID session,int offset,int limit) { return new History(snapshot(session),turns(session,offset,limit)); }
    List<Event> events(UUID session, long after);
    Optional<Event> append(UUID session, UUID turn, String type, Map<String,Object> data);
    void compact(Session expected, String summary, long through, int tokens);
    void usage(UUID student, UUID classId, UUID turn, String provider, String purpose, ModelUsage usage);
    List<Usage> usage(UUID classId);
    void expire();
}
