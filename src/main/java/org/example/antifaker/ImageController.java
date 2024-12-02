package org.example.antifaker;

import jakarta.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.InternalException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequiredArgsConstructor
public class ImageController {

    private final RestClient restClient;
    private final String pythonServerUrl = "localhost:8000/process_image";


    // GET /upload - 업로드 폼 페이지 표시
    @GetMapping("/upload")
    public String uploadForm() {
        return "upload";  // upload.html 템플릿을 반환
    }

//    // POST /send-image - 워터마크 추가 및 이미지 반환
//    @PostMapping("/send-image")
//    public ResponseEntity<String> processImage(
//        @RequestParam("file") MultipartFile file,
//        HttpSession session,
//        @RequestParam Integer sliderValue
//        ) {
//        System.out.println(sliderValue);
//        try {
//            if (file.isEmpty()) {
//                System.err.println("업로드된 파일이 비어 있습니다.");
//                return ResponseEntity.badRequest().body("파일이 비어 있습니다.");
//            }
//
//            BufferedImage originalImage = ImageIO.read(file.getInputStream());
//            if (originalImage == null) {
//                System.err.println("이미지 파일을 읽을 수 없습니다.");
//                return ResponseEntity.badRequest().body("유효하지 않은 이미지 파일입니다.");
//            }
//
//            // 흑백 이미지 변환
//            BufferedImage grayImage = new BufferedImage(
//                originalImage.getWidth(),
//                originalImage.getHeight(),
//                BufferedImage.TYPE_BYTE_GRAY);
//            grayImage.getGraphics().drawImage(originalImage, 0, 0, null);
//
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            ImageIO.write(grayImage, "jpg", baos);
//
//            byte[] imageBytes = baos.toByteArray();
//
//            // 세션에 저장
//            session.setAttribute("processedImage", imageBytes);
//
//            // Base64로 응답 데이터 생성
//            String base64Image =
//                "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(imageBytes);
//
//            System.out.println("Base64 Encoded Image Length: " + base64Image.length());
//            System.out.println("이미지 응답 준비 완료");
//
//            return ResponseEntity.ok(base64Image);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body("이미지 처리 중 오류가 발생했습니다.");
//        }
//    }


    public ResponseEntity<String> processImage(
        @RequestParam("file") MultipartFile file,
        HttpSession session,
        @RequestParam Integer sliderValue
    ) {
        System.out.println(sliderValue);
        try {
            // MultipartBodyBuilder를 사용해 이미지와 슬라이더 값 함께 전송
            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
            bodyBuilder.part("file", file.getResource(), MediaType.parseMediaType(file.getContentType()));
            bodyBuilder.part("epsilon", sliderValue, MediaType.TEXT_PLAIN);

            ResponseEntity<String> result = restClient.post()
                .uri(pythonServerUrl)
                .body(bodyBuilder.build())
                .retrieve()
                .toEntity(String.class);

            if (result.getStatusCode().value() == 422) {
                throw new InternalException("입력 이미지에서 얼굴을 인식하지 못했습니다.");
            }

            String base64Result = result.getBody();

            // Base64 문자열을 byte 배열로 변환
            byte[] imageBytes = Base64.getDecoder().decode(base64Result);

            // 세션에 저장
            session.setAttribute("processedImage", imageBytes);

            return ResponseEntity.ok(base64Result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("이미지 처리 중 오류가 발생했습니다.");
        }
    }

    // GET /complete - 완료 페이지 표시
    @GetMapping("/complete")
    public String complete(Model model, HttpSession session) {
        // 세션에서 이미지 데이터 가져오기
        byte[] imageBytes = (byte[]) session.getAttribute("processedImage");
        if (imageBytes != null) {
            // Base64로 인코딩하여 뷰에 전달
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            model.addAttribute("processedImage", base64Image);
        }
        return "complete";  // complete.html 템플릿을 반환
    }

    // GET /download-image - 처리된 이미지 다운로드
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
        String base64Image = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(imageBytes);
        return ResponseEntity.ok(base64Image);
    }

//    @GetMapping("/processed-image")
//    public ResponseEntity<String> getProcessedImage(HttpSession session) {
//        try {
//            // 5초 동안 처리 시간을 시뮬레이션
//            Thread.sleep(5000);
//        } catch (InterruptedException e) {
//            // 에러 발생 시 로그 기록
//            Thread.currentThread().interrupt();
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body("이미지 처리 중 문제가 발생했습니다.");
//        }
//
//        byte[] imageBytes = (byte[]) session.getAttribute("processedImage");
//
//        if (imageBytes == null) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                .body("이미지가 아직 처리되지 않았습니다.");
//        }
//
//        String base64Image = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(imageBytes);
//        return ResponseEntity.ok(base64Image);
//    }

}