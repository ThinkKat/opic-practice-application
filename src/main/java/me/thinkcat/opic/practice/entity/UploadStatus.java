package me.thinkcat.opic.practice.entity;

/**
 * 답변 오디오 파일의 업로드 상태
 */
public enum UploadStatus {
    /**
     * 업로드 대기 중 (Presigned URL 발급됨, 파일 미업로드)
     */
    PENDING,

    /**
     * 업로드 완료 (S3에 파일 존재 확인됨)
     */
    SUCCESS,

    /**
     * 업로드 실패 (타임아웃 또는 명시적 실패)
     */
    FAILED
}
