package vn.hoctapcanman.core.reminder.infrastructure.push;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import vn.hoctapcanman.core.reminder.application.dto.PushDto.*;
import vn.hoctapcanman.core.reminder.application.port.PushSender;

/** Payloadless Web Push (RFC 8030) + ES256 VAPID (RFC 8292). The worker owns the generic message. */
@Component
public class WebPushSenderAdapter implements PushSender {
    private final String publicKey,privateKey,subject;
    private final Clock clock;
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private static final Base64.Encoder B64=Base64.getUrlEncoder().withoutPadding();
    public WebPushSenderAdapter(@Value("${VAPID_PUBLIC_KEY:}") String publicKey,@Value("${VAPID_PRIVATE_KEY:}") String privateKey,@Value("${VAPID_SUBJECT:}") String subject,Clock clock) {
        this.publicKey=publicKey;this.privateKey=privateKey;this.subject=subject;this.clock=clock;
    }
    public Config config(){
        try {
            if(publicKey.isBlank()||privateKey.isBlank()||subject.isBlank())return new Config(false,"");
            authorization(URI.create("https://fcm.googleapis.com"));
            return new Config(true,publicKey);
        } catch(GeneralSecurityException|IllegalArgumentException ignored){return new Config(false,"");}
    }
    String authorization(URI endpoint) throws GeneralSecurityException {
        URI contact=URI.create(subject);
        if(!Set.of("mailto","https").contains(contact.getScheme()))throw new IllegalArgumentException("VAPID subject");
        byte[] raw=Base64.getUrlDecoder().decode(publicKey);
        byte[] scalar=Base64.getUrlDecoder().decode(privateKey);
        if(raw.length!=65||raw[0]!=4||scalar.length!=32)throw new IllegalArgumentException("VAPID keys");
        var parameters=AlgorithmParameters.getInstance("EC");parameters.init(new ECGenParameterSpec("secp256r1"));
        var curve=parameters.getParameterSpec(ECParameterSpec.class);var factory=KeyFactory.getInstance("EC");
        var secret=factory.generatePrivate(new ECPrivateKeySpec(new BigInteger(1,scalar),curve));
        var point=new ECPoint(new BigInteger(1,Arrays.copyOfRange(raw,1,33)),new BigInteger(1,Arrays.copyOfRange(raw,33,65)));
        var publicEc=factory.generatePublic(new ECPublicKeySpec(point,curve));
        String header=B64.encodeToString("{\"typ\":\"JWT\",\"alg\":\"ES256\"}".getBytes(StandardCharsets.UTF_8));
        String claims=B64.encodeToString(JSON.writeValueAsBytes(Map.of("aud",endpoint.getScheme()+"://"+endpoint.getAuthority(),"exp",clock.instant().plusSeconds(3600).getEpochSecond(),"sub",subject)));
        String input=header+"."+claims;
        var signer=Signature.getInstance("SHA256withECDSAinP1363Format");signer.initSign(secret);signer.update(input.getBytes(StandardCharsets.US_ASCII));
        byte[] signature=signer.sign();
        var verifier=Signature.getInstance("SHA256withECDSAinP1363Format");verifier.initVerify(publicEc);verifier.update(input.getBytes(StandardCharsets.US_ASCII));
        if(!verifier.verify(signature))throw new IllegalArgumentException("VAPID key pair");
        return "vapid t="+input+"."+B64.encodeToString(signature)+", k="+publicKey;
    }
    public int send(Subscription s) {
        try {
            vn.hoctapcanman.core.reminder.application.service.PushService.validateEndpoint(s.endpoint());
            var endpoint=URI.create(s.endpoint());
            var request=HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(10)).header("TTL","3600")
                .header("Authorization",authorization(endpoint)).POST(HttpRequest.BodyPublishers.noBody()).build();
            return client.send(request,HttpResponse.BodyHandlers.discarding()).statusCode();
        } catch(InterruptedException ex){Thread.currentThread().interrupt();return 503;}
        catch(Exception ignored){return 503;}
    }
}
