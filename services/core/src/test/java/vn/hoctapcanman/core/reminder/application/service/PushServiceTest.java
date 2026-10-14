package vn.hoctapcanman.core.reminder.application.service;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import java.time.*;
import org.junit.jupiter.api.*;
import vn.hoctapcanman.core.reminder.application.dto.PushDto.*;
import vn.hoctapcanman.core.reminder.application.port.*;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.workspace.application.port.WorkspaceStore;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.Item;
class PushServiceTest {
 final PushStore store=mock(PushStore.class);final PushSender sender=mock(PushSender.class);final WorkspaceStore workspace=mock(WorkspaceStore.class);final ClassMembership membership=mock(ClassMembership.class);
 final UUID student=UUID.randomUUID(),id=UUID.randomUUID();final Instant now=Instant.parse("2026-10-14T00:00:00Z");final Subscription subscription=new Subscription("https://fcm.googleapis.com/send/example",new Keys("synthetic","synthetic"));
 final PushService service=new PushService(store,sender,workspace,membership,Clock.fixed(now,ZoneOffset.UTC));
 @BeforeEach void setup(){when(sender.config()).thenReturn(new Config(true,"public"));when(store.targets()).thenReturn(List.of(new Target(id,student,subscription)));when(membership.lopHoc(student)).thenReturn(Optional.of(UUID.randomUUID()));when(workspace.reminders(student,now)).thenReturn(List.of(new Item("r1","Lịch","","",Map.of("startsAt",now.toString()))));}
 @Test void rejectsArbitraryUrlsAndAcceptsBrowserServices(){for(String url:List.of("http://fcm.googleapis.com/x","https://localhost/x","https://fcm.googleapis.com.evil.test/x","https://fcm.googleapis.com:443/x","https://user@fcm.googleapis.com/x"))assertThatThrownBy(()->PushService.validateEndpoint(url)).isInstanceOf(RuntimeException.class);PushService.validateEndpoint(subscription.endpoint());}
 @Test void recordsOnlySuccessfulDelivery(){when(sender.send(subscription)).thenReturn(201);service.dispatch();verify(store).mark(eq(id),anyString(),eq(now));}
 @Test void failedDeliveryRemainsRetryable(){when(sender.send(subscription)).thenReturn(503);service.dispatch();verify(store,never()).mark(any(),any(),any());verify(store,never()).remove(any());}
 @Test void expiredSubscriptionIsRemoved(){when(sender.send(subscription)).thenReturn(410);service.dispatch();verify(store).remove(id);}
 @Test void duplicateReminderIsNotSent(){when(store.delivered(eq(id),anyString())).thenReturn(true);service.dispatch();verify(sender,never()).send(any());}
 @Test void missingKeysKeepFallbackAndDoNotSend(){when(sender.config()).thenReturn(new Config(false,""));service.dispatch();verifyNoInteractions(store,workspace);}
}
