package com.tenco.blog.util;

import com.tenco.blog._core.errors.Exception400;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

// IoC 안함 (파일 기능 처리에만 동작할  수 있도록 static 메서드로 구현 예정)
public class FileUtil {

    // saveFile에 매개변수로 항상 주소를 입력하는건 좋지않음
    // 업로드 될 파일 경로를 미지 상수로 지정 /resources에 저장 시키도록 할 예정
    public static final String IMAGES_DIR = "C:\\upload";

    // 기능
    // 1. 파일 저장 기능 String이 반환타입인 이유 - 서버 컴에 저장되어 있는 파일 위치를 반환하기 때문
    public static String saveFile(MultipartFile file , String uploadDir) throws IOException {
        // 1단계 : 파일 유효성 검사 - 파일이 없거나 크기가 0이면 오류
        if (file == null || file.isEmpty()){
            return null; // 프로필 이미지 업로드는 선택 사항
        }

        // 2단계 : 파일 업로드 경로 생성 ( + 존재 여부 확인)
        // Path : 파일 시스템 경로를 나타내는 객체
        // Path.get() : 문자열 경로를 Path 객체로 변환해주는 객체
        Path uploadPath = Paths.get(IMAGES_DIR);

        // Path 객체를 이용해 디렉토리(폴더)가 없으면 자동 생성 해준다.
        // Files.exists() : 파일 디렉토리 존재 여부 확인
        if (Files.exists(uploadPath) == false){
            // 현재 서버 컴퓨터에 images/* 폴더가 존재하지 않는 상태임
            Files.createDirectories(uploadPath); // 상위 폴더까지 자동 생성 해준다.(예외 메서드에서 던짐)
        }

        // 3단계 : 원본 파일 이름 가져오기
        // 중복 이름의 덮어쓰기 방지 - 방어적 코드
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()){
            throw new Exception400("파일명이 없습니다.");
        }

        // 4단계 : UUID를 사용한 고유 파일명 생성
        String uuid = UUID.randomUUID().toString(); // 난수 발생
        String savedFileName = uuid + "_" + originalFilename;
        // ex : "19-7180-7750_popo.png " 파일명으로 재 생성 됨

        // 5단계 : 메모리상에 존재하는 파일 데이터를 로컬 컴퓨터(디스크)에 저장
        // 5.1 : 우리가 만든 파일 폴더경로 + 재생성한 파일이름 --> 정확한 위치에 파일이 생성 됨
        // ex : image/213-2323-1232_a.png
        Path filePath = uploadPath.resolve(savedFileName); // 주소(파일)

        Files.copy(file.getInputStream(),filePath); // inputStream, 저장 시킬 주소

        return savedFileName;


    } // end of saveFile

    // 2. 파일 삭제 기능

    // 3. 편의 기능 추가 예정 (이미지 파일이 맞는지 확인)
    public static boolean isImageFile(MultipartFile file){
        if (file == null || file.isEmpty()){
            return false;
        }

        // pdf, hwp <-- 막아 줘야한다. how? - mine타입 이용
        String contentType = file.getContentType(); // image/png, application/pdf
        boolean isImage = contentType.startsWith("image/");  // images-mime파일일 경우만 true
        return isImage;

    } // end of isImageFile

}
