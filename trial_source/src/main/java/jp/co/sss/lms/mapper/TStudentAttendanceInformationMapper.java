package jp.co.sss.lms.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import jp.co.sss.lms.dto.AttendanceInformationDto;

/**
 * 勤怠情報確認画面用マッパー
 * 
 * @author 峠伸治 Task.57
 */
@Mapper
public interface TStudentAttendanceInformationMapper {
	/**
	 * @param courseName
	 * @param placeId
	 * @param companyName
	 * @param userName
	 * @param hddenFlg
	 * @param deleteFlg
	 * @return 勤怠情報確認画面用DTOリスト
	 * @author 峠伸治 - Task.57
	 */
	List<AttendanceInformationDto> getAttendanceInformation(@Param("courseName") String courseName,
			@Param("placeId") Integer placeId, @Param("companyName") String companyName,
			@Param("userName") String userName, @Param("role") String role,
			@Param("deleteFlg") Short deleteFlg);
}
