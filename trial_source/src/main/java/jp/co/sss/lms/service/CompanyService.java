package jp.co.sss.lms.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jp.co.sss.lms.dto.CompanyDto;
import jp.co.sss.lms.mapper.MCompanyMapper;
import jp.co.sss.lms.util.Constants;

/**
 * 企業情報サービス
 * 
 * @author 峠伸治 - Task.57
 */
@Service
public class CompanyService {
	@Autowired
	private MCompanyMapper mCompanyMapper;
	
	/**
	 * 企業情報サービス 企業情報の全検索
	 * 
	 * @return companyDtoList
	 * @author 峠伸治 - Task.57
	 */
	public List<CompanyDto> getCompanyDtoList(){
		
		List<CompanyDto> companyDtoList = mCompanyMapper.getCompanyDto(Constants.DB_FLG_FALSE);
		
		return companyDtoList;
	}
}
