package jp.co.sss.lms.dto;

import lombok.Data;

/**
 * 勤怠情報確認画面用DTO
 * 
 * @author 峠伸治 - Task.57
 */
@Data
public class AttendanceInformationDto {
	
	/** LMSユーザID */
	private Integer lmsUserId;
	/** ユーザID */
	private Integer userId;
	/** ユーザ名 */
	private String userName;
	/** コース名 */
	private String courceName;
	/** 会場名 */
	private String placeName;
	/** 企業名 */
	private String companyName;
}
