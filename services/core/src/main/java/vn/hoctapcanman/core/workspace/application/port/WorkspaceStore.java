package vn.hoctapcanman.core.workspace.application.port;

import java.time.Instant;
import java.util.*;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.*;

public interface WorkspaceStore {
    List<Item> classes(UUID actor,boolean admin);
    Dashboard dashboard(UUID classId);
    List<Item> students(UUID classId);
    List<Item> history(UUID student,UUID classId);
    List<Item> bank(UUID classId);
    List<Item> assignments(UUID classId);
    List<Item> modes(UUID student,UUID classId);
    List<Item> recommendations(UUID student,UUID classId);
    Library library(UUID classId,boolean teacher);
    List<Item> reviews(UUID classId);
    void assign(UUID actor,Assign request,List<UUID> students,Instant now);
    void recommend(UUID actor,NextProblem request,Instant now);
    void override(UUID actor,MasteryOverride request,Instant now);
    void document(UUID actor,UUID classId,String title,String license,String text,Instant now);
    void formula(UUID actor,Formula request,Instant now);
    Settings settings(UUID classId);
    void settings(UUID actor,Settings request,Instant now);
    List<Item> schedule(UUID student);
    void schedule(UUID student,Schedule request);
    boolean complete(UUID student,UUID id);
    List<Item> reminders(UUID student,Instant now);
    String accountRole(UUID actor);
    boolean managesUser(UUID actor,UUID target);
    List<Item> users();
    void createUser(UUID actor,NewUser request,String hash,Instant now);
    void bulk(UUID actor,Bulk request,Instant now);
    void createClass(UUID actor,NewClass request,Instant now);
    void enroll(UUID actor,Enrollment request,Instant now);
    List<Item> audit();
    void log(UUID actor,String action,String target,String reason,Instant now);
    Optional<TutorContext> tutorContext(UUID student,UUID classId,TutorRequest request);
    boolean testMode(UUID student,UUID classId,String code);
}
