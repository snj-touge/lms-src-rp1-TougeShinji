package jp.co.sss.lms.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;

import jp.co.sss.lms.dto.AttendanceInformationDto;
import jp.co.sss.lms.dto.AttendanceManagementDto;
import jp.co.sss.lms.dto.LoginUserDto;
import jp.co.sss.lms.entity.TStudentAttendance;
import jp.co.sss.lms.enums.AttendanceStatusEnum;
import jp.co.sss.lms.form.AttendanceForm;
import jp.co.sss.lms.form.DailyAttendanceForm;
import jp.co.sss.lms.mapper.TStudentAttendanceInformationMapper;
import jp.co.sss.lms.mapper.TStudentAttendanceMapper;
import jp.co.sss.lms.util.AttendanceUtil;
import jp.co.sss.lms.util.Constants;
import jp.co.sss.lms.util.DateUtil;
import jp.co.sss.lms.util.LoginUserUtil;
import jp.co.sss.lms.util.MessageUtil;
import jp.co.sss.lms.util.TrainingTime;

/**
 * 勤怠情報（受講生入力）サービス
 * 
 * @author 東京ITスクール
 * @author 峠伸治 - Task.25
 * @author 峠伸治 - Task.26
 * @author 峠伸治 - Task.27
 * @author 峠伸治 - Task.57
 */
@Service
public class StudentAttendanceService {

	@Autowired
	private DateUtil dateUtil;
	@Autowired
	private AttendanceUtil attendanceUtil;
	@Autowired
	private MessageUtil messageUtil;
	@Autowired
	private LoginUserUtil loginUserUtil;
	@Autowired
	private LoginUserDto loginUserDto;
	@Autowired
	private TStudentAttendanceMapper tStudentAttendanceMapper;
	@Autowired
	private TStudentAttendanceInformationMapper tStudentAttendanceInformationMapper;

	/**
	 * 勤怠一覧情報取得
	 * 
	 * @param courseId
	 * @param lmsUserId
	 * @return 勤怠管理画面用DTOリスト
	 */
	public List<AttendanceManagementDto> getAttendanceManagement(Integer courseId,
			Integer lmsUserId) {

		// 勤怠管理リストの取得
		List<AttendanceManagementDto> attendanceManagementDtoList = tStudentAttendanceMapper
				.getAttendanceManagement(courseId, lmsUserId, Constants.DB_FLG_FALSE);
		for (AttendanceManagementDto dto : attendanceManagementDtoList) {
			// 中抜け時間を設定
			if (dto.getBlankTime() != null) {
				TrainingTime blankTime = attendanceUtil.calcBlankTime(dto.getBlankTime());
				dto.setBlankTimeValue(blankTime.getFormattedString());
			}
			// 遅刻早退区分判定
			AttendanceStatusEnum statusEnum = AttendanceStatusEnum.getEnum(dto.getStatus());
			if (statusEnum != null) {
				dto.setStatusDispName(statusEnum.name);
			}
		}
		return attendanceManagementDtoList;
	}

	/**
	 * 出退勤更新前のチェック
	 * 
	 * @param attendanceType
	 * @return エラーメッセージ
	 */
	public String punchCheck(Short attendanceType) {
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 権限チェック
		if (!loginUserUtil.isStudent()) {
			return messageUtil.getMessage(Constants.VALID_KEY_AUTHORIZATION);
		}
		// 研修日チェック
		if (!attendanceUtil.isWorkDay(loginUserDto.getCourseId(), trainingDate)) {
			return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_NOTWORKDAY);
		}
		// 登録情報チェック
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		switch (attendanceType) {
		case Constants.CODE_VAL_ATWORK:
			if (tStudentAttendance != null
					&& !tStudentAttendance.getTrainingStartTime().equals("")) {
				// 本日の勤怠情報は既に入力されています。直接編集してください。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHALREADYEXISTS);
			}
			break;
		case Constants.CODE_VAL_LEAVING:
			if (tStudentAttendance == null
					|| tStudentAttendance.getTrainingStartTime().equals("")) {
				// 出勤情報がないため退勤情報を入力出来ません。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHINEMPTY);
			}
			if (!tStudentAttendance.getTrainingEndTime().equals("")) {
				// 本日の勤怠情報は既に入力されています。直接編集してください。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHALREADYEXISTS);
			}
			TrainingTime trainingStartTime = new TrainingTime(
					tStudentAttendance.getTrainingStartTime());
			TrainingTime trainingEndTime = new TrainingTime();
			if (trainingStartTime.compareTo(trainingEndTime) > 0) {
				// 退勤時刻は出勤時刻より後でなければいけません。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_TRAININGTIMERANGE);
			}
			break;
		}
		return null;
	}

	/**
	 * 出勤ボタン処理
	 * 
	 * @return 完了メッセージ
	 */
	public String setPunchIn() {
		// 当日日付
		Date date = new Date();
		// 本日の研修日
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 現在の研修時刻
		TrainingTime trainingStartTime = new TrainingTime();
		// 遅刻早退ステータス
		AttendanceStatusEnum attendanceStatusEnum = attendanceUtil.getStatus(trainingStartTime,
				null);
		// 研修日の勤怠情報取得
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		if (tStudentAttendance == null) {
			// 登録処理
			tStudentAttendance = new TStudentAttendance();
			tStudentAttendance.setLmsUserId(loginUserDto.getLmsUserId());
			tStudentAttendance.setTrainingDate(trainingDate);
			tStudentAttendance.setTrainingStartTime(trainingStartTime.toString());
			tStudentAttendance.setTrainingEndTime("");
			tStudentAttendance.setStatus(attendanceStatusEnum.code);
			tStudentAttendance.setNote("");
			tStudentAttendance.setAccountId(loginUserDto.getAccountId());
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			tStudentAttendance.setFirstCreateUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setFirstCreateDate(date);
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			tStudentAttendance.setBlankTime(null);
			tStudentAttendanceMapper.insert(tStudentAttendance);
		} else {
			// 更新処理
			tStudentAttendance.setTrainingStartTime(trainingStartTime.toString());
			tStudentAttendance.setStatus(attendanceStatusEnum.code);
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			tStudentAttendanceMapper.update(tStudentAttendance);
		}
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 退勤ボタン処理
	 * 
	 * @return 完了メッセージ
	 */
	public String setPunchOut() {
		// 当日日付
		Date date = new Date();
		// 本日の研修日
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 研修日の勤怠情報取得
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		// 出退勤時刻
		TrainingTime trainingStartTime = new TrainingTime(
				tStudentAttendance.getTrainingStartTime());
		TrainingTime trainingEndTime = new TrainingTime();
		// 遅刻早退ステータス
		AttendanceStatusEnum attendanceStatusEnum = attendanceUtil.getStatus(trainingStartTime,
				trainingEndTime);
		// 更新処理
		tStudentAttendance.setTrainingEndTime(trainingEndTime.toString());
		tStudentAttendance.setStatus(attendanceStatusEnum.code);
		tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
		tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
		tStudentAttendance.setLastModifiedDate(date);
		tStudentAttendanceMapper.update(tStudentAttendance);
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 勤怠フォームへ設定
	 * 
	 * @param attendanceManagementDtoList
	 * @return 勤怠編集フォーム
	 * @author 峠伸治 - Task26
	 */
	public AttendanceForm setAttendanceForm(
			List<AttendanceManagementDto> attendanceManagementDtoList) {
		//峠伸治 - Task.26

		//時間マップ作成
		Map<Integer, String> hourMap = new LinkedHashMap<>();
		hourMap.put(null, "");
		for (int i = 0; i < 24; i++) {
			hourMap.put(i, String.format("%02d", i));
		}
		//分マップ作成
		Map<Integer, String> minuteMap = new LinkedHashMap<>();
		minuteMap.put(null, "");
		for (int i = 0; i < 60; i++) {
			minuteMap.put(i, String.format("%02d", i));
		}

		AttendanceForm attendanceForm = new AttendanceForm();
		attendanceForm.setAttendanceList(new ArrayList<DailyAttendanceForm>());
		attendanceForm.setLmsUserId(loginUserDto.getLmsUserId());
		attendanceForm.setUserName(loginUserDto.getUserName());
		attendanceForm.setLeaveFlg(loginUserDto.getLeaveFlg());
		attendanceForm.setBlankTimes(attendanceUtil.setBlankTime());

		// 途中退校している場合のみ設定
		if (loginUserDto.getLeaveDate() != null) {
			attendanceForm
					.setLeaveDate(dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy-MM-dd"));
			attendanceForm.setDispLeaveDate(
					dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy年M月d日"));
		}

		// 勤怠管理リストの件数分、日次の勤怠フォームに移し替え
		for (AttendanceManagementDto attendanceManagementDto : attendanceManagementDtoList) {
			DailyAttendanceForm dailyAttendanceForm = new DailyAttendanceForm();
			dailyAttendanceForm.setStudentAttendanceId(attendanceManagementDto.getStudentAttendanceId());
			dailyAttendanceForm.setTrainingDate(dateUtil.toString(attendanceManagementDto.getTrainingDate()));
			//			dailyAttendanceForm.setTrainingStartTime(attendanceManagementDto.getTrainingStartTime());
			//出勤時間を変換
			if (attendanceManagementDto.getTrainingStartTime() != null) {
				TrainingTime trainingStartTime = new TrainingTime(attendanceManagementDto.getTrainingStartTime());
				//時間と分をそれぞれ分解
				dailyAttendanceForm.setTrainingStartTimeHour(hourMap.get(trainingStartTime.getHour()));
				dailyAttendanceForm.setTrainingStartTimeMinute(minuteMap.get(trainingStartTime.getMinute()));
			}
			//			dailyAttendanceForm.setTrainingEndTiTrainingTimeme(attendanceManagementDto.getTrainingEndTime());
			//退勤時間を変換
			if (attendanceManagementDto.getTrainingEndTime() != null) {
				TrainingTime trainingEndTime = new TrainingTime(attendanceManagementDto.getTrainingEndTime());
				//時間と分をそれぞれ分解
				dailyAttendanceForm.setTrainingEndTimeHour(hourMap.get(trainingEndTime.getHour()));
				dailyAttendanceForm.setTrainingEndTimeMinute(minuteMap.get(trainingEndTime.getMinute()));
			}
			if (attendanceManagementDto.getBlankTime() != null) {
				dailyAttendanceForm.setBlankTime(attendanceManagementDto.getBlankTime());
				dailyAttendanceForm.setBlankTimeValue(
						String.valueOf(attendanceUtil.calcBlankTime(attendanceManagementDto.getBlankTime())));
			}
			dailyAttendanceForm.setStatus(String.valueOf(attendanceManagementDto.getStatus()));
			dailyAttendanceForm.setNote(attendanceManagementDto.getNote());
			dailyAttendanceForm.setSectionName(attendanceManagementDto.getSectionName());
			dailyAttendanceForm.setIsToday(attendanceManagementDto.getIsToday());
			dailyAttendanceForm.setDispTrainingDate(dateUtil
					.dateToString(attendanceManagementDto.getTrainingDate(), "yyyy年M月d日(E)"));
			dailyAttendanceForm.setStatusDispName(attendanceManagementDto.getStatusDispName());

			attendanceForm.getAttendanceList().add(dailyAttendanceForm);
		}
		return attendanceForm;
	}

	/**
	 * 勤怠登録・更新処理
	 * 
	 * @param attendanceForm
	 * @return 完了メッセージ
	 * @throws ParseException
	 */
	public String update(AttendanceForm attendanceForm) throws ParseException {

		Integer lmsUserId = loginUserUtil.isStudent() ? loginUserDto.getLmsUserId()
				: attendanceForm.getLmsUserId();

		// 現在の勤怠情報（受講生入力）リストを取得
		List<TStudentAttendance> tStudentAttendanceList = tStudentAttendanceMapper
				.findByLmsUserId(lmsUserId, Constants.DB_FLG_FALSE);

		// 入力された情報を更新用のエンティティに移し替え
		Date date = new Date();
		for (DailyAttendanceForm dailyAttendanceForm : attendanceForm.getAttendanceList()) {

			// 更新用エンティティ作成
			TStudentAttendance tStudentAttendance = new TStudentAttendance();
			// 日次勤怠フォームから更新用のエンティティにコピー
			BeanUtils.copyProperties(dailyAttendanceForm, tStudentAttendance);
			// 研修日付
			tStudentAttendance
					.setTrainingDate(dateUtil.parse(dailyAttendanceForm.getTrainingDate()));
			// 現在の勤怠情報リストのうち、研修日が同じものを更新用エンティティで上書き
			for (TStudentAttendance entity : tStudentAttendanceList) {
				if (entity.getTrainingDate().equals(tStudentAttendance.getTrainingDate())) {
					tStudentAttendance = entity;
					break;
				}
			}
			tStudentAttendance.setLmsUserId(lmsUserId);
			tStudentAttendance.setAccountId(loginUserDto.getAccountId());
			// 出勤時刻整形
			TrainingTime trainingStartTime = null;
			trainingStartTime = new TrainingTime(dailyAttendanceForm.getTrainingStartTime());
			tStudentAttendance.setTrainingStartTime(trainingStartTime.getFormattedString());
			// 退勤時刻整形
			TrainingTime trainingEndTime = null;
			trainingEndTime = new TrainingTime(dailyAttendanceForm.getTrainingEndTime());
			tStudentAttendance.setTrainingEndTime(trainingEndTime.getFormattedString());
			// 中抜け時間
			tStudentAttendance.setBlankTime(dailyAttendanceForm.getBlankTime());
			// 遅刻早退ステータス
			if ((trainingStartTime != null || trainingEndTime != null)
					&& !dailyAttendanceForm.getStatusDispName().equals("欠席")) {
				AttendanceStatusEnum attendanceStatusEnum = attendanceUtil
						.getStatus(trainingStartTime, trainingEndTime);
				tStudentAttendance.setStatus(attendanceStatusEnum.code);
			}
			// 備考
			tStudentAttendance.setNote(dailyAttendanceForm.getNote());
			// 更新者と更新日時
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			// 削除フラグ
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			// 登録用Listへ追加
			tStudentAttendanceList.add(tStudentAttendance);
		}
		// 登録・更新処理
		for (TStudentAttendance tStudentAttendance : tStudentAttendanceList) {
			if (tStudentAttendance.getStudentAttendanceId() == null) {
				tStudentAttendance.setFirstCreateUser(loginUserDto.getLmsUserId());
				tStudentAttendance.setFirstCreateDate(date);
				tStudentAttendanceMapper.insert(tStudentAttendance);
			} else {
				tStudentAttendanceMapper.update(tStudentAttendance);
			}
		}
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 勤怠の未入力の検索
	 * 
	 * @return　過去日に未入力があるかの有無
	 * @throws ParseException
	 * @author 峠伸治 – Task.25
	 */
	public boolean notEnterCount() throws ParseException {
		//検索用のエンティティ作成
		TStudentAttendance tStudentAttendance = new TStudentAttendance();
		//日付のフォーマットパターンを設定
		SimpleDateFormat sdf = new SimpleDateFormat(Constants.DEFAULT_DATE_FORMAT);
		//今日の日付を取得
		Date today = new Date();
		String dateString = sdf.format(today);
		today = sdf.parse(dateString);
		//登録処理
		tStudentAttendance.setLmsUserId(loginUserDto.getUserId());
		tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
		tStudentAttendance.setTrainingDate(today);
		//過去日の未入力数をカウント
		Integer count = tStudentAttendanceMapper.notEnterCount(tStudentAttendance);
		//未入力カウント数が0より大きい場合、trueを返す
		if (count > 0) {
			return true;
		}
		//未入力カウント数が0の場合、falseを返す
		return false;

	}

	/**
	 * 出退勤の時間と分の結合
	 * 
	 * @param attendanceForm
	 * @author 峠伸治 - Task.26
	 */
	public void formatConversion(AttendanceForm attendanceForm) {
		for (DailyAttendanceForm dailyAttendanceForm : attendanceForm.getAttendanceList()) {
			//出勤時間を変換(未入力の場合は行わない)

			if ((!StringUtils.isEmpty(dailyAttendanceForm.getTrainingStartTimeHour()))
					&& (!StringUtils.isEmpty(dailyAttendanceForm.getTrainingStartTimeMinute()))) {
				//時間と分を結合(HH:mm形式)
				TrainingTime trainingStartTime = new TrainingTime(
						Integer.parseInt(dailyAttendanceForm.getTrainingStartTimeHour()),
						Integer.parseInt(dailyAttendanceForm.getTrainingStartTimeMinute()));
				dailyAttendanceForm.setTrainingStartTime(trainingStartTime.getFormattedString());
			} else {
				dailyAttendanceForm.setTrainingStartTime(null);
			}
			//退勤時間を変換(未入力の場合は行わない)
			if ((!StringUtils.isEmpty(dailyAttendanceForm.getTrainingEndTimeHour()))
					&& (!StringUtils.isEmpty(dailyAttendanceForm.getTrainingEndTimeMinute()))) {
				//時間と分を結合(HH:mm形式)
				TrainingTime trainingEndTime = new TrainingTime(
						Integer.parseInt(dailyAttendanceForm.getTrainingEndTimeHour()),
						Integer.parseInt(dailyAttendanceForm.getTrainingEndTimeMinute()));
				dailyAttendanceForm.setTrainingEndTime(trainingEndTime.getFormattedString());
			} else {
				dailyAttendanceForm.setTrainingEndTime(null);
			}
		}

	}

	/**
	 * 勤怠情報の入力チェック
	 * 
	 * @param attendanceForm
	 * @param result
	 * @author 峠伸治 - Task.27
	 * @throws ParseException 
	 */
	public void updateInputCheck(AttendanceForm attendanceForm, BindingResult result) throws ParseException {
		for (int i = 0; i < attendanceForm.getAttendanceList().size(); i++) {
			TrainingTime startTime;
			TrainingTime endTime;
			DailyAttendanceForm dailyAttendanceForm = attendanceForm.getAttendanceList().get(i);
			//備考欄の文字数エラー追加
			if (dailyAttendanceForm.getNote().length() > 100) {
				result.rejectValue("attendanceList[" + i + "].note", Constants.VALID_KEY_MAXBYTELENGTH,
						new String[] { "備考", "100" }, null);
			}
			//出勤時間が片方未入力
			if (StringUtils.isEmpty(dailyAttendanceForm.getTrainingStartTimeHour())
					&& !dailyAttendanceForm.getTrainingStartTimeMinute().isBlank()) {
				result.rejectValue("attendanceList[" + i + "].trainingStartTimeHour", Constants.INPUT_INVALID,
						new String[] { "出勤時間" }, null);
			}
			if (!StringUtils.isEmpty(dailyAttendanceForm.getTrainingStartTimeHour())
					&& StringUtils.isEmpty(dailyAttendanceForm.getTrainingStartTimeMinute())) {
				result.rejectValue("attendanceList[" + i + "].trainingStartTimeMinute", Constants.INPUT_INVALID,
						new String[] { "出勤時間" }, null);
			}
			//退勤時間が片方未入力
			if (StringUtils.isEmpty(dailyAttendanceForm.getTrainingEndTimeHour())
					&& !StringUtils.isEmpty(dailyAttendanceForm.getTrainingEndTimeMinute())) {
				result.rejectValue("attendanceList[" + i + "].trainingEndTimeHour", Constants.INPUT_INVALID,
						new String[] { "退勤時間" }, null);
			}
			if (!StringUtils.isEmpty(dailyAttendanceForm.getTrainingEndTimeHour())
					&& StringUtils.isEmpty(dailyAttendanceForm.getTrainingEndTimeMinute())) {
				result.rejectValue("attendanceList[" + i + "].trainingEndTimeMinute", Constants.INPUT_INVALID,
						new String[] { "退勤時間" }, null);
			}
			//出勤時間に入力なし　＆　退勤時間に入力あり
			if (StringUtils.isEmpty(dailyAttendanceForm.getTrainingStartTime())
					&& !StringUtils.isEmpty(dailyAttendanceForm.getTrainingEndTime())) {
				result.rejectValue("attendanceList[" + i + "].trainingStartTimeHour",
						Constants.VALID_KEY_ATTENDANCE_PUNCHINEMPTY,
						null, null);
			}
			//出退勤の時間が比較できる
			if (!StringUtils.isEmpty(dailyAttendanceForm.getTrainingEndTime())
					&& !StringUtils.isEmpty(dailyAttendanceForm.getTrainingStartTime())) {
				startTime = new TrainingTime(dailyAttendanceForm.getTrainingStartTime());
				endTime = new TrainingTime(dailyAttendanceForm.getTrainingEndTime());
				//出勤時間　>　退勤時間の場合
				if (startTime.max(endTime, startTime).equals(startTime)) {
					result.rejectValue("attendanceList[" + i + "].trainingEndTimeHour",
							Constants.VALID_KEY_ATTENDANCE_TRAININGTIMERANGE,
							new String[] { Integer.toString(i) }, null);
				}
				//出勤時間 + 中抜け時間　>　退勤時間の場合
				if (dailyAttendanceForm.getBlankTime() != null) {
					startTime = startTime.add(attendanceUtil.calcBlankTime(dailyAttendanceForm.getBlankTime()));
					if (startTime.max(endTime, startTime).equals(startTime)) {
						result.rejectValue("attendanceList[" + i + "].blankTime",
								Constants.VALID_KEY_ATTENDANCE_BLANKTIMEERROR,
								null, null);
					}
				}
			}

		}

	}

	/**
	 * 勤怠情報一覧を取得
	 * 
	 * @param courseName
	 * @param companyName
	 * @param userName
	 * @return attendanceInformationDtoList
	 * @author 峠伸治 - Task.57
	 */
	public List<AttendanceInformationDto> getAttendanceInformationList(String courseName, String companyName,
			String userName) {
		List<AttendanceInformationDto> attendanceInformationDtoList = tStudentAttendanceInformationMapper
				.getAttendanceInformation(courseName,
						loginUserDto.getPlaceId(), companyName, userName, Constants.CODE_VAL_ROLL_STUDENT,
						Constants.DB_FLG_FALSE);
		return attendanceInformationDtoList;
	}

	/**
	 * 勤怠の未入力の検索(講師用)
	 * 
	 * @return　過去日に未入力があるかの有無
	 * @throws ParseException
	 * @author 峠伸治 - Task.57
	 * @param lmsUserId 
	 */
	public boolean searchNotEnterCount(Integer lmsUserId) throws ParseException {
		TStudentAttendance tStudentAttendance = new TStudentAttendance();
		//日付のフォーマットパターンを設定
		SimpleDateFormat sdf = new SimpleDateFormat(Constants.DEFAULT_DATE_FORMAT);
		//今日の日付を取得
		Date today = new Date();
		String dateString = sdf.format(today);
		today = sdf.parse(dateString);
		//登録処理
		tStudentAttendance.setLmsUserId(lmsUserId);
		tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
		tStudentAttendance.setTrainingDate(today);
		//過去日の未入力数をカウント
		Integer count = tStudentAttendanceMapper.notEnterCount(tStudentAttendance);
		//未入力カウント数が0より大きい場合、trueを返す
		if (count > 0) {
			return true;
		}
		//未入力カウント数が0の場合、falseを返す
		return false;
	}

}
