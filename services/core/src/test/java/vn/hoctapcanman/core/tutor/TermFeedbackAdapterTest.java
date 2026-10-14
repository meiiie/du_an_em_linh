package vn.hoctapcanman.core.tutor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import vn.hoctapcanman.core.shared.infrastructure.math.*;
import vn.hoctapcanman.core.tutor.infrastructure.client.TermFeedbackAdapter;

class TermFeedbackAdapterTest {
    final MathServiceClient math=mock(MathServiceClient.class);
    final TermFeedbackAdapter adapter=new TermFeedbackAdapter(math);
    Map<String,Object> row(int index,String text,String status) {return Map.of("index",index,"text",text,"status",status);}
    @Test void callsBoundedSandboxAndReturnsOnlyStudentTerms() {
        when(math.call(eq(MathJob.SO_HANG),any())).thenReturn(new MathResult.Ok(Map.of("terms",List.of(row(1,"2x","ok"),row(2,"-6x","sai"),row(3,"+4","thua")),"missing_count",0)));
        var result=adapter.derivative("x**2-6*x+4","2x -6x +4").orElseThrow();
        assertEquals("sai",result.terms().get(1).status());
        verify(math).call(MathJob.SO_HANG,Map.of("function","x**2-6*x+4","student","2x -6x +4","variable","x"));
        assertEquals(6,MathJob.SO_HANG.timeout().toSeconds());
    }
    @Test void malformedOrFailureNeverAddsAHint() {
        for(var body:List.<Map<String,Object>>of(Map.of(),Map.of("terms",List.of(row(1,"2x","ok")),"missing_count",-1),
            Map.of("terms",List.of(row(2,"2x","ok")),"missing_count",0),
            Map.of("terms",List.of(row(1,"2x","DAT")),"missing_count",0),
            Map.of("terms",List.of(row(1,"expected value","sai")),"missing_count",0),
            Map.of("terms",List.of(row(1,"2x","ok")),"missing_count",1.5))) {
            when(math.call(any(),any())).thenReturn(new MathResult.Ok(body));
            assertTrue(adapter.derivative("x**2","2x").isEmpty());
        }
        when(math.call(any(),any())).thenReturn(new MathResult.Failed(MathResult.Reason.TIMEOUT,"Hết giờ"));
        assertTrue(adapter.derivative("x**2","2x").isEmpty());
    }
}
