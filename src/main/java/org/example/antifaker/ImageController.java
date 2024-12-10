package org.example.antifaker;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import java.util.Base64;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Controller
@Slf4j
@RequiredArgsConstructor
public class ImageController {

    private final RestClient restClient;

    @GetMapping("/upload")
    public String uploadForm() {
        return "upload";
    }

    @PostMapping("/send-image")
    public ResponseEntity<String> processImage(
        @RequestParam("file") MultipartFile file,
        HttpSession session,
        @RequestParam Integer sliderValue
    ) {
        try {
            MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();

            parts.add("input_image", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });
            parts.add("epsilon", String.valueOf(sliderValue));

            ResponseEntity<String> result = restClient.post()
                .uri("http://35.213.183.114:8000/process_image/")
                .body(parts)
                .retrieve()
                .onStatus(httpStatusCode -> httpStatusCode.value() == 422, ((request, response) -> {
                    throw new IllegalStateException("입력 이미지에서 얼굴을 인식하지 못했습니다.");
                }))
                .toEntity(String.class);

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> responseMap = objectMapper.readValue(result.getBody(), Map.class);

            String base64Result = responseMap.get("result");

            byte[] imageBytes = Base64.getDecoder()
                .decode(base64Result.split(",")[base64Result.contains(",") ? 1 : 0]);

            session.setAttribute("processedImage", imageBytes);

            return ResponseEntity.ok(base64Result);
        } catch (Exception e) {
            log.error("이미지 처리 중 예상치 못한 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(e.getMessage());
        }
    }

    @GetMapping("/complete")
    public String complete(Model model, HttpSession session) {
        byte[] imageBytes = (byte[]) session.getAttribute("processedImage");
        if (imageBytes != null) {
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            model.addAttribute("processedImage", base64Image);
        }
        return "complete";
    }

    @GetMapping("/download-image")
    public ResponseEntity<byte[]> downloadImage(HttpSession session) {
        byte[] imageBytes = (byte[]) session.getAttribute("processedImage");
        if (imageBytes == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        headers.setContentDisposition(ContentDisposition.builder("attachment")
            .filename("processed_image.jpg")
            .build());

        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/processed-image")
    public ResponseEntity<String> getProcessedImage(HttpSession session) {
        byte[] imageBytes = (byte[]) session.getAttribute("processedImage");

        if (imageBytes == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("이미지가 아직 처리되지 않았습니다.");
        }
        String base64Image =
            "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(imageBytes);
        return ResponseEntity.ok(base64Image);
    }
}