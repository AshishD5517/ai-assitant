package coderarmy.in.avatarDemo;

import io.livekit.server.AccessToken;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/livekit")
public class LiveKitTokenController {

    @Value("${livekit.url}")
    private String livekitUrl;

    @Value("${livekit.api-key}")
    private String apiKey;

    @Value("${livekit.api-secret}")
    private String apiSecret;

    @PostMapping("/token")
    public Map<String, String> createToken(@RequestBody Map<String, String> request) {

        String roomName = request.get("roomName");

        if (roomName == null || roomName.isBlank()) {
            roomName = "avatar-" + UUID.randomUUID();
        }

        String participantIdentity = "user-" + UUID.randomUUID();

        AccessToken token = new AccessToken(apiKey, apiSecret);

        token.setIdentity(participantIdentity);
        token.setName("Coder Army User");

        token.addGrants(new RoomJoin(true), new RoomName(roomName));

        token.setTtl(60 * 60 * 1000L);

        Map<String, String> response = new HashMap<>();
        response.put("serverUrl", livekitUrl);
        response.put("token", token.toJwt());
        response.put("roomName", roomName);

        return response;
    }
}
