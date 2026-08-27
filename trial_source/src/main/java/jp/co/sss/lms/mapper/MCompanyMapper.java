package jp.co.sss.lms.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import jp.co.sss.lms.dto.CompanyDto;
/**
 * 企業マスタマッパー
 * 
 * @author 峠伸治 - Task.57
 */
@Mapper
public interface MCompanyMapper {
	
	/**
	 * 企業情報全件取得
	 * 
	 * @param deleteFlg
	 * @return 企業情報DTOリスト
	 */
	List<CompanyDto> getCompanyDto(@Param("deleteFlg") Short deleteFlg);
}
