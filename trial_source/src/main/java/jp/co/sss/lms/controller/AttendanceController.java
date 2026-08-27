package jp.co.sss.lms.controller;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jp.co.sss.lms.dto.AttendanceInformationDto;
import jp.co.sss.lms.dto.AttendanceManagementDto;
import jp.co.sss.lms.dto.CompanyDto;
import jp.co.sss.lms.dto.CourseDto;
import jp.co.sss.lms.dto.LoginUserDto;
import jp.co.sss.lms.form.AttendanceForm;
import jp.co.sss.lms.service.CompanyService;
import jp.co.sss.lms.service.CourseService;
import jp.co.sss.lms.service.StudentAttendanceService;
import jp.co.sss.lms.util.AttendanceUtil;
import jp.co.sss.lms.util.Constants;

/**
 * 勤怠管理コントローラ
 * 
 * @author 東京ITスクール
 * @author 峠伸治 - Task.25
 * @author 峠伸治 - Task.26
 * @author 峠伸治 - Task.27
 * @author 峠伸治 - Task.57
 */
@Controller
@RequestMapping("/attendance")
public class AttendanceController {

	@Autowired
	private StudentAttendanceService studentAttendanceService;
	//検索候補取得用のサービスクラス
	@Autowired
	private CourseService courseService;
	@Autowired
	private CompanyService companyService;
	@Autowired
	private LoginUserDto loginUserDto;
	@Autowired
	private AttendanceUtil attendanceUtil;

	/**
	 * 勤怠管理画面 初期表示
	 * 
	 * @param lmsUserId
	 * @param courseId
	 * @param model
	 * @return 勤怠管理画面
	 * @throws ParseException
	 * @author 峠伸治 - Task.25
	 * @author 峠伸治 - Task.26
	 * @author 峠伸治 - Task.27
	 * @author 峠伸治 - Task.57
	 */
	@RequestMapping(path = "/detail", method = RequestMethod.GET)
	public String index(Model model) throws ParseException {

		// 勤怠一覧の取得
		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
		//峠伸治 Task.25
		//過去日に未入力があるか検索
		boolean isNotEnter = studentAttendanceService.notEnterCount();
		//過去日に未入力があるか表示
		model.addAttribute("isNotEnter", isNotEnter);
		model.addAttribute("attendanceManagementDtoList", attendanceManagementDtoList);

		return "attendance/detail";
	}

	/**
	 * 勤怠管理画面 『出勤』ボタン押下
	 * 
	 * @param model
	 * @return 勤怠管理画面
	 */
	@RequestMapping(path = "/detail", params = "punchIn", method = RequestMethod.POST)
	public String punchIn(Model model) {

		// 更新前のチェック
		String error = studentAttendanceService.punchCheck(Constants.CODE_VAL_ATWORK);
		model.addAttribute("error", error);
		// 勤怠登録
		if (error == null) {
			String message = studentAttendanceService.setPunchIn();
			model.addAttribute("message", message);
		}
		// 一覧の再取得
		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
		model.addAttribute("attendanceManagementDtoList", attendanceManagementDtoList);

		return "attendance/detail";
	}

	/**
	 * 勤怠管理画面 『退勤』ボタン押下
	 * 
	 * @param model
	 * @return 勤怠管理画面
	 */
	@RequestMapping(path = "/detail", params = "punchOut", method = RequestMethod.POST)
	public String punchOut(Model model) {

		// 更新前のチェック
		String error = studentAttendanceService.punchCheck(Constants.CODE_VAL_LEAVING);
		model.addAttribute("error", error);
		// 勤怠登録
		if (error == null) {
			String message = studentAttendanceService.setPunchOut();
			model.addAttribute("message", message);
		}
		// 一覧の再取得
		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
		model.addAttribute("attendanceManagementDtoList", attendanceManagementDtoList);

		return "attendance/detail";
	}

	/**
	 * 勤怠管理画面 『勤怠情報を直接編集する』リンク押下
	 * 
	 * @param model
	 * @return 勤怠情報直接変更画面
	 */
	@RequestMapping(path = "/update")
	public String update(Model model) {

		// 勤怠管理リストの取得
		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
		// 勤怠フォームの生成
		AttendanceForm attendanceForm = studentAttendanceService
				.setAttendanceForm(attendanceManagementDtoList);
		model.addAttribute("attendanceForm", attendanceForm);

		return "attendance/update";
	}

	/**
	 * 勤怠情報直接変更画面 『更新』ボタン押下
	 * 
	 * @param attendanceForm
	 * @param model
	 * @param result
	 * @return 勤怠管理画面
	 * @throws ParseException
	 * @author 峠伸治 - Task.26
	 * @author 峠伸治 - Task.27
	 */
	@RequestMapping(path = "/update", params = "complete", method = RequestMethod.POST)
	public String complete(AttendanceForm attendanceForm, Model model, BindingResult result)
			throws ParseException {
		//峠伸治 - Task.26
		//時間と分を結合
		studentAttendanceService.formatConversion(attendanceForm);
		//峠伸治 - Task.27
		studentAttendanceService.updateInputCheck(attendanceForm, result);
		if (result.hasErrors()) {
			attendanceForm.setBlankTimes(attendanceUtil.setBlankTime());
			model.addAttribute("attendanceForm", attendanceForm);
			return "attendance/update";
		}
		// 更新
		String message = studentAttendanceService.update(attendanceForm);
		model.addAttribute("message", message);
		// 一覧の再取得
		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
		model.addAttribute("attendanceManagementDtoList", attendanceManagementDtoList);

		return "attendance/detail";
	}

	/**
	 * 勤怠情報確認画面 初期表示
	 * 
	 * @param model
	 * @return 勤怠情報確認画面
	 * @author 峠伸治 - Task.57
	 */
	@RequestMapping(path = "/list", method = RequestMethod.GET)
	public String list(Model model) {
		//会場名と自身のlmsユーザIDから検索
		List<AttendanceInformationDto> attendanceInformationDtoList = studentAttendanceService
				.getAttendanceInformationList(null, null, null);
		//検索候補リスト作成
		List<String> courseNameList = new ArrayList<String>();
		List<String> companyNameList = new ArrayList<String>();
		for (CourseDto courseDto : courseService.getCourseDtoList()) {
			courseNameList.add(courseDto.getCourseName());
		}
		for (CompanyDto companyDto : companyService.getCompanyDtoList()) {
			companyNameList.add(companyDto.getCompanyName());
		}
		//検索用の変数を追加
		model.addAttribute("courseName", "");
		model.addAttribute("placeName", loginUserDto.getPlaceName());
		model.addAttribute("companyName", "");
		model.addAttribute("userName", "");
		//検索候補用のリストを追加
		model.addAttribute("courseList", courseNameList);
		model.addAttribute("companyList", companyNameList);
		//検索欄のリストを追加
		model.addAttribute("attendanceInformationList", attendanceInformationDtoList);

		return "attendance/list";
	}

	/**
	 * 勤怠情報確認画面 『検索』ボタン押下
	 * 
	 * @param courseName
	 * @param companyName
	 * @param userName
	 * @param model
	 * @return 勤怠情報確認画面
	 * @author 峠伸治 - Task.57
	 */
	@RequestMapping(path = "/list", method = RequestMethod.POST)
	public String search(String courseName, String companyName,String userName,Model model) {
		//受け取った内容と会場名、自身のlmsユーザIDから検索
		List<AttendanceInformationDto> attendanceInformationDtoList = studentAttendanceService
				.getAttendanceInformationList(courseName,companyName,userName);
		List<String> courseNameList = new ArrayList<String>();
		List<String> companyNameList = new ArrayList<String>();
		for (CourseDto courseDto : courseService.getCourseDtoList()) {
			courseNameList.add(courseDto.getCourseName());
		}
		for (CompanyDto companyDto : companyService.getCompanyDtoList()) {
			companyNameList.add(companyDto.getCompanyName());
		}
		//検索用の変数を追加
		model.addAttribute("courseName", courseName);
		model.addAttribute("placeName", loginUserDto.getPlaceName());
		model.addAttribute("companyName", companyName);
		model.addAttribute("userName", userName);
		//検索候補用のリストを追加
		model.addAttribute("courseList", courseNameList);
		model.addAttribute("companyList", companyNameList);
		//検索欄のリストを追加
		model.addAttribute("attendanceInformationList", attendanceInformationDtoList);
		
		return "attendance/list";
	}
}