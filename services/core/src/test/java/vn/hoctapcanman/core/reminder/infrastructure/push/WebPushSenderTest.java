package vn.hoctapcanman.core.reminder.infrastructure.push;
import static org.assertj.core.api.Assertions.*;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;
import java.math.BigInteger;
import java.util.*;
import java.time.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
class WebPushSenderTest {
 byte[] fixed(BigInteger n){byte[] value=n.toByteArray();return Arrays.copyOfRange(value,Math.max(0,value.length-32),value.length);}
 @Test void vapidSignatureUsesCorrectAudienceExpiryAndPublicKey() throws Exception {
  var generator=KeyPairGenerator.getInstance("EC");generator.initialize(new ECGenParameterSpec("secp256r1"));var pair=generator.generateKeyPair();
  var point=((ECPublicKey)pair.getPublic()).getW();var raw=new byte[65];raw[0]=4;
  var x=fixed(point.getAffineX());var y=fixed(point.getAffineY());System.arraycopy(x,0,raw,33-x.length,x.length);System.arraycopy(y,0,raw,65-y.length,y.length);
  var scalar=fixed(((ECPrivateKey)pair.getPrivate()).getS());var b64=Base64.getUrlEncoder().withoutPadding();
  byte[] privateRaw=new byte[32];System.arraycopy(scalar,0,privateRaw,32-scalar.length,scalar.length);
  var now=Instant.parse("2026-10-14T00:00:00Z");var adapter=new WebPushSenderAdapter(b64.encodeToString(raw),b64.encodeToString(privateRaw),"mailto:demo@example.test",Clock.fixed(now,ZoneOffset.UTC));
  assertThat(adapter.config().available()).isTrue();
  String auth=adapter.authorization(URI.create("https://fcm.googleapis.com/send/example"));String jwt=auth.substring(8,auth.indexOf(", k="));String[] segments=jwt.split("\\.");
  var claims=JsonMapper.builder().build().readTree(Base64.getUrlDecoder().decode(segments[1]));assertThat(claims.path("aud").asString()).isEqualTo("https://fcm.googleapis.com");assertThat(claims.path("exp").asLong()).isEqualTo(now.plusSeconds(3600).getEpochSecond());
  var verifier=Signature.getInstance("SHA256withECDSAinP1363Format");verifier.initVerify(pair.getPublic());verifier.update((segments[0]+"."+segments[1]).getBytes(StandardCharsets.US_ASCII));assertThat(verifier.verify(Base64.getUrlDecoder().decode(segments[2]))).isTrue();
 }
 @Test void absentAndInvalidKeysNeverExposePublicKey(){assertThat(new WebPushSenderAdapter("bad","bad","mailto:demo@example.test",Clock.systemUTC()).config().available()).isFalse();assertThat(new WebPushSenderAdapter("","","",Clock.systemUTC()).config().publicKey()).isEmpty();}
}
