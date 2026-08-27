package jp.co.sss.lms.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import jp.co.sss.lms.dto.CourseDto;
import jp.co.sss.lms.dto.CourseServiceCourseDto;

/**
 * コースマスタマッパー
 * 
 * @author 東京ITスクール
 * @author 峠伸治 - Task.57
 */
@Mapper
public interface MCourseMapper {

	/**
	 * コース詳細取得
	 * 
	 * @param courseId
	 * @param lmsUserId
	 * @param deleteFlg
	 * @return コース情報サービス コースDTO
	 */
	CourseServiceCourseDto getCourseDetail(@Param("courseId") Integer courseId,
			@Param("deleteFlg") Short deleteFlg);

	/**
	 * コース数取得
	 * 
	 * @param courseId
	 * @return コース数
	 */
	Integer getCourseCount(Integer courseId);

	/**
	 * コース情報の全検索
	 * 
	 * @param deleteFlg
	 * @return コースDTO
	 * @author 峠伸治 - Task.57
	 */
	List<CourseDto> getCouseDto(@Param("deleteFlg") Short deleteFlg);
}
