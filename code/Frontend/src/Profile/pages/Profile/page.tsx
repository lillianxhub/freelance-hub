import { useEffect, useState, type ChangeEvent, type FormEvent } from "react";
import Button from "../../../components/Button";
import FormLabel from "../../../components/FormLabel";
import Input from "../../../components/Input";
import PageHeader from "../../../components/PageHeader";
import Toast from "../../../components/Toast";
import { ErrorState, LoadingState } from "../../../components/ViewState";
import { useProfile } from "../../useProfile";
import type { ChangePasswordInput, Profile, ProfileFieldErrors } from "../../../types/profile";
import type { ResourceInput } from "../../../types/workspace";
import type { ToastMessage } from "../../../types/toast";
import type { FieldErrorProps } from "../../../types/ui";
import { getErrorMessage } from "../../../api/apiError";
import { changePassword } from "../../../services/profile";
import { updateProfile } from "../../../services/workspace";
import {
  loadThaiAddressData,
  type ThaiProvince,
} from "../../../services/thaiAddress";
import { validateProfileFields } from "../../profile.validators";

const emptyProfile: ResourceInput<"profiles"> = {
  full_name: "",
  display_name: "",
  first_name: "",
  last_name: "",
  email: "",
  phone: "",
  address: "",
  city: "",
  country: "",
  postal_code: "",
  province: "",
  district: "",
  sub_district: "",
  tax_id: "",
  logo_url: "",
  bank_name: "",
  bank_account_name: "",
  bank_account_number: "",
  timezone: "Asia/Bangkok",
  currency: "THB",
  date_format: "DD/MM/YYYY",
  default_tax_rate: 0,
  default_hourly_rate: 0,
  bio: "",
};

const editableProfileFields: Array<keyof ResourceInput<"profiles">> = [
  "display_name",
  "first_name",
  "last_name",
  "phone",
  "tax_id",
  "address",
  "province",
  "district",
  "sub_district",
  "postal_code",
];

function fieldValue(value: unknown): string {
  return value === null || value === undefined ? "" : String(value);
}

function FieldError({ id, message }: FieldErrorProps) {
  return message ? <p id={id} className="field-error">{message}</p> : null;
}

function ProfilePage() {
  const { data, loading, error, refresh } = useProfile();
  const [draft, setDraft] = useState<
    Profile | ResourceInput<"profiles"> | null
  >(null);
  const [toast, setToast] = useState<ToastMessage | null>(null);
  const [saving, setSaving] = useState(false);
  const [provinces, setProvinces] = useState<ThaiProvince[]>([]);
  const [addressLoading, setAddressLoading] = useState(true);
  const [addressError, setAddressError] = useState("");
  const [profileErrors, setProfileErrors] = useState<ProfileFieldErrors>({});
  const [passwordForm, setPasswordForm] = useState<ChangePasswordInput>({
    current_password: "",
    new_password: "",
    confirm_password: "",
  });
  const [passwordSaving, setPasswordSaving] = useState(false);

  useEffect(() => {
    let active = true;
    loadThaiAddressData()
      .then((items) => {
        if (active) setProvinces(items);
      })
      .catch((loadError: unknown) => {
        if (active)
          setAddressError(
            getErrorMessage(loadError, "ไม่สามารถโหลดข้อมูลจังหวัดได้"),
          );
      })
      .finally(() => {
        if (active) setAddressLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  if (loading) return <LoadingState label="กำลังโหลดโปรไฟล์..." />;
  if (error) return <ErrorState message={error} onRetry={refresh} />;

  const profile = draft || data.profiles[0] || emptyProfile;
  const selectedProvince = provinces.find(
    (item) => item.name_th === profile.province,
  );
  const districts = selectedProvince?.districts || [];
  const selectedDistrict = districts.find(
    (item) => item.name_th === profile.district,
  );
  const subDistricts = selectedDistrict?.sub_districts || [];
  const selectedSubDistrict = subDistricts.find(
    (item) => item.name_th === profile.sub_district,
  );
  const savedProfile = data.profiles[0] || emptyProfile;
  const hasProfileChanges = draft !== null && editableProfileFields.some(
    (field) => fieldValue(profile[field]) !== fieldValue(savedProfile[field]),
  );
  const passwordTooShort = passwordForm.new_password.length > 0 && passwordForm.new_password.length < 8;
  const passwordMismatch = passwordForm.confirm_password.length > 0 &&
    passwordForm.new_password !== passwordForm.confirm_password;
  const passwordUnchanged = passwordForm.new_password.length > 0 &&
    passwordForm.current_password === passwordForm.new_password;
  const passwordFormInvalid = !passwordForm.current_password ||
    !passwordForm.new_password ||
    !passwordForm.confirm_password ||
    passwordTooShort ||
    passwordMismatch ||
    passwordUnchanged;

  const updateFields = (values: Partial<ResourceInput<"profiles">>) => {
    setDraft((current) => ({
      ...(current || data.profiles[0] || emptyProfile),
      ...values,
    }));
  };

  const update = (
    event: ChangeEvent<
      HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement
    >,
  ) => {
    const { name, value } = event.target;
    setDraft((current) => ({
      ...(current || data.profiles[0] || emptyProfile),
      [name]: value,
    }));
  };

  const updateDigits = (maxLength: number) => (event: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;
    updateFields({ [name]: value.replace(/\D/g, "").slice(0, maxLength) });
  };

  const updateProvince = (event: ChangeEvent<HTMLSelectElement>) => {
    updateFields({
      province: event.target.value,
      district: "",
      sub_district: "",
      postal_code: "",
    });
  };

  const updateDistrict = (event: ChangeEvent<HTMLSelectElement>) => {
    updateFields({
      district: event.target.value,
      sub_district: "",
      postal_code: "",
    });
  };

  const updateSubDistrict = (event: ChangeEvent<HTMLSelectElement>) => {
    const subDistrict = subDistricts.find(
      (item) => item.name_th === event.target.value,
    );
    updateFields({
      sub_district: event.target.value,
      postal_code: subDistrict ? String(subDistrict.zip_code) : "",
    });
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const validationErrors = validateProfileFields(profile);
    setProfileErrors(validationErrors);
    if (Object.keys(validationErrors).length > 0) return;
    setSaving(true);
    try {
      const response = await updateProfile({
        ...profile,
        default_tax_rate: Number(profile.default_tax_rate) || 0,
        default_hourly_rate: Number(profile.default_hourly_rate) || 0,
      });
      await refresh();
      setDraft(null);
      setToast({
        success: response.success,
        message: response.message || "บันทึกโปรไฟล์เรียบร้อยแล้ว",
      });
    } catch (err) {
      setToast({
        success: false,
        message: getErrorMessage(err, "ไม่สามารถบันทึกโปรไฟล์ได้"),
      });
    } finally {
      setSaving(false);
    }
  };

  const updatePassword = (event: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;
    setPasswordForm((current) => ({ ...current, [name]: value }));
  };

  const submitPassword = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (passwordForm.new_password.length < 8) {
      setToast({
        success: false,
        message: "รหัสผ่านใหม่ต้องมีอย่างน้อย 8 ตัวอักษร",
      });
      return;
    }
    if (passwordForm.new_password !== passwordForm.confirm_password) {
      setToast({ success: false, message: "ยืนยันรหัสผ่านใหม่ไม่ตรงกัน" });
      return;
    }
    if (passwordForm.current_password === passwordForm.new_password) {
      setToast({
        success: false,
        message: "รหัสผ่านใหม่ต้องไม่ซ้ำกับรหัสผ่านเดิม",
      });
      return;
    }
    setPasswordSaving(true);
    try {
      const response = await changePassword(
        passwordForm.current_password,
        passwordForm.new_password,
      );
      setPasswordForm({
        current_password: "",
        new_password: "",
        confirm_password: "",
      });
      setToast({
        success: response.success,
        message: response.message || "เปลี่ยนรหัสผ่านเรียบร้อยแล้ว",
      });
    } catch (passwordError) {
      setToast({
        success: false,
        message: getErrorMessage(passwordError, "ไม่สามารถเปลี่ยนรหัสผ่านได้"),
      });
    } finally {
      setPasswordSaving(false);
    }
  };

  return (
    <div className="page-view profile-page">
      <PageHeader
        eyebrow="บัญชีของฉัน"
        title="ข้อมูลส่วนตัว"
        description="ข้อมูลที่ใช้แสดงในบัญชีและเอกสารของคุณ"
      />

      <form className="profile-layout" onSubmit={submit} noValidate>
        <section className="panel profile-form-card">
          <div className="form-grid profile-contact-grid">
            <div className="form-field full">
              <FormLabel htmlFor="profile-display-name" required>ชื่อที่แสดง</FormLabel>
              <Input
                id="profile-display-name"
                name="display_name"
                value={profile.display_name || ""}
                onChange={update}
                aria-invalid={Boolean(profileErrors.display_name)}
                aria-describedby={profileErrors.display_name ? "profile-display-name-error" : undefined}
                required
              />
              <FieldError id="profile-display-name-error" message={profileErrors.display_name} />
              <small>ชื่อนี้จะแสดงบนหน้าเว็บและเมนูบัญชี</small>
            </div>
            <div className="form-field">
              <FormLabel htmlFor="profile-first-name" required>ชื่อ</FormLabel>
              <Input
                id="profile-first-name"
                name="first_name"
                value={profile.first_name || ""}
                onChange={update}
                aria-invalid={Boolean(profileErrors.first_name)}
                aria-describedby={profileErrors.first_name ? "profile-first-name-error" : undefined}
                required
              />
              <FieldError id="profile-first-name-error" message={profileErrors.first_name} />
            </div>
            <div className="form-field">
              <FormLabel htmlFor="profile-last-name" required>นามสกุล</FormLabel>
              <Input
                id="profile-last-name"
                name="last_name"
                value={profile.last_name || ""}
                onChange={update}
                aria-invalid={Boolean(profileErrors.last_name)}
                aria-describedby={profileErrors.last_name ? "profile-last-name-error" : undefined}
                required
              />
              <FieldError id="profile-last-name-error" message={profileErrors.last_name} />
            </div>
            <div className="form-field">
              <FormLabel htmlFor="profile-email">อีเมลบัญชี</FormLabel>
              <Input
                id="profile-email"
                name="email"
                type="email"
                value={profile.email || ""}
                disabled
                aria-disabled="true"
              />
              <small>อีเมลนี้ใช้สำหรับเข้าสู่ระบบ</small>
            </div>
            <div className="form-field">
              <FormLabel htmlFor="profile-phone" required>เบอร์โทรศัพท์</FormLabel>
              <Input
                id="profile-phone"
                name="phone"
                value={profile.phone || ""}
                onChange={updateDigits(10)}
                inputMode="numeric"
                pattern="[0-9]{0,10}"
                maxLength={10}
                aria-invalid={Boolean(profileErrors.phone)}
                aria-describedby={profileErrors.phone ? "profile-phone-error" : undefined}
              />
              <FieldError id="profile-phone-error" message={profileErrors.phone} />
              <small>กรอกตัวเลขได้ไม่เกิน 10 หลัก</small>
            </div>
            <div className="form-field">
              <FormLabel htmlFor="profile-tax">เลขประจำตัวผู้เสียภาษี</FormLabel>
              <Input
                id="profile-tax"
                name="tax_id"
                value={profile.tax_id || ""}
                onChange={updateDigits(13)}
                inputMode="numeric"
                pattern="[0-9]{0,13}"
                maxLength={13}
                aria-invalid={Boolean(profileErrors.tax_id)}
                aria-describedby={profileErrors.tax_id ? "profile-tax-error" : undefined}
              />
              <FieldError id="profile-tax-error" message={profileErrors.tax_id} />
              <small>กรอกตัวเลขได้ไม่เกิน 13 หลัก</small>
            </div>
            {/* <div className="form-field full">
              <label htmlFor="profile-bio">แนะนำตัว</label>
              <textarea
                id="profile-bio"
                name="bio"
                value={profile.bio || ""}
                onChange={update}
                maxLength={1000}
                placeholder="เขียนแนะนำตัวหรือรายละเอียดการทำงานโดยย่อ"
              />
            </div> */}
          </div>

          <div className="profile-divider" />
          <p className="profile-section-label">ที่อยู่จัดส่งเอกสาร</p>
          {addressError && <p className="form-message error">{addressError}</p>}

          <div className="form-grid profile-address-grid">
            <div className="form-field full">
              <FormLabel htmlFor="profile-address" required>ที่อยู่</FormLabel>
              <Input
                id="profile-address"
                name="address"
                value={profile.address || ""}
                onChange={update}
                aria-invalid={Boolean(profileErrors.address)}
                aria-describedby={profileErrors.address ? "profile-address-error" : undefined}
              />
              <FieldError id="profile-address-error" message={profileErrors.address} />
            </div>
            <div className="form-field">
              <FormLabel htmlFor="profile-province" required>จังหวัด</FormLabel>
              <select
                id="profile-province"
                value={profile.province || ""}
                onChange={updateProvince}
                disabled={addressLoading || Boolean(addressError)}
                aria-invalid={Boolean(profileErrors.province)}
                aria-describedby={profileErrors.province ? "profile-province-error" : undefined}
              >
                <option value="">
                  {addressLoading ? "กำลังโหลดจังหวัด..." : "เลือกจังหวัด"}
                </option>
                {provinces.map((province) => (
                  <option key={province.id} value={province.name_th}>
                    {province.name_th}
                  </option>
                ))}
              </select>
              <FieldError id="profile-province-error" message={profileErrors.province} />
            </div>
            <div className="form-field">
              <FormLabel htmlFor="profile-district" required>อำเภอ / เขต</FormLabel>
              <select
                id="profile-district"
                value={profile.district || ""}
                onChange={updateDistrict}
                disabled={!selectedProvince}
                aria-invalid={Boolean(profileErrors.district)}
                aria-describedby={profileErrors.district ? "profile-district-error" : undefined}
              >
                <option value="">เลือกอำเภอ / เขต</option>
                {districts.map((district) => (
                  <option key={district.id} value={district.name_th}>
                    {district.name_th}
                  </option>
                ))}
              </select>
              <FieldError id="profile-district-error" message={profileErrors.district} />
            </div>
            <div className="form-field">
              <FormLabel htmlFor="profile-sub-district" required>ตำบล / แขวง</FormLabel>
              <select
                id="profile-sub-district"
                value={profile.sub_district || ""}
                onChange={updateSubDistrict}
                disabled={!selectedDistrict}
                aria-invalid={Boolean(profileErrors.sub_district)}
                aria-describedby={profileErrors.sub_district ? "profile-sub-district-error" : undefined}
              >
                <option value="">เลือกตำบล / แขวง</option>
                {subDistricts.map((subDistrict) => (
                  <option key={subDistrict.id} value={subDistrict.name_th}>
                    {subDistrict.name_th}
                  </option>
                ))}
              </select>
              <FieldError id="profile-sub-district-error" message={profileErrors.sub_district} />
            </div>
            <div className="form-field">
              <FormLabel htmlFor="profile-postal-code" required>รหัสไปรษณีย์</FormLabel>
              <select
                id="profile-postal-code"
                name="postal_code"
                value={profile.postal_code || ""}
                disabled={!selectedSubDistrict}
                onChange={update}
                aria-invalid={Boolean(profileErrors.postal_code)}
                aria-describedby={profileErrors.postal_code ? "profile-postal-code-error" : undefined}
              >
                <option value="">เลือกตำบลก่อน</option>
                {selectedSubDistrict && (
                  <option value={String(selectedSubDistrict.zip_code)}>
                    {selectedSubDistrict.zip_code}
                  </option>
                )}
              </select>
              <FieldError id="profile-postal-code-error" message={profileErrors.postal_code} />
            </div>
          </div>

          <div className="profile-form-actions">
            <Button
              variant="secondary"
              type="button"
              onClick={() => setDraft(null)}
            >
              ยกเลิก
            </Button>
            <Button
              variant="primary"
              type="submit"
              disabled={saving || !hasProfileChanges}
            >
              {saving ? "กำลังบันทึก..." : "บันทึกการเปลี่ยนแปลง"}
            </Button>
          </div>
        </section>
      </form>

      <form className="panel password-card" onSubmit={submitPassword}>
        <div className="panel-heading">
          <div>
            <h2>เปลี่ยนรหัสผ่าน</h2>
            <p>ใช้รหัสผ่านเดิมเพื่อกำหนดรหัสผ่านใหม่สำหรับเข้าสู่ระบบ</p>
          </div>
        </div>
        <div className="form-grid password-grid">
          <div className="form-field">
            <FormLabel htmlFor="current-password" required>รหัสผ่านเดิม</FormLabel>
            <Input
              id="current-password"
              name="current_password"
              type="password"
              value={passwordForm.current_password}
              onChange={updatePassword}
              autoComplete="current-password"
              required
            />
          </div>
          <div className="form-field">
            <FormLabel htmlFor="new-password" required>รหัสผ่านใหม่</FormLabel>
            <Input
              id="new-password"
              name="new_password"
              type="password"
              value={passwordForm.new_password}
              onChange={updatePassword}
              autoComplete="new-password"
              minLength={8}
              aria-invalid={passwordTooShort || passwordUnchanged}
              required
            />
            {passwordTooShort ? (
              <small className="field-error">รหัสผ่านใหม่ต้องมีอย่างน้อย 8 ตัวอักษร</small>
            ) : (
              <small>ต้องมีอย่างน้อย 8 ตัวอักษร</small>
            )}
            {passwordUnchanged && (
              <small className="field-error">รหัสผ่านใหม่ต้องไม่ซ้ำกับรหัสผ่านเดิม</small>
            )}
          </div>
          <div className="form-field">
            <FormLabel htmlFor="confirm-password" required>ยืนยันรหัสผ่านใหม่</FormLabel>
            <Input
              id="confirm-password"
              name="confirm_password"
              type="password"
              value={passwordForm.confirm_password}
              onChange={updatePassword}
              autoComplete="new-password"
              minLength={8}
              aria-invalid={passwordMismatch}
              required
            />
            {passwordMismatch && (
              <small className="field-error">ยืนยันรหัสผ่านใหม่ไม่ตรงกัน</small>
            )}
          </div>
        </div>
        <div className="profile-form-actions">
          <Button
            variant="primary"
            type="submit"
            disabled={passwordSaving || passwordFormInvalid}
          >
            {passwordSaving ? "กำลังเปลี่ยนรหัสผ่าน..." : "เปลี่ยนรหัสผ่าน"}
          </Button>
        </div>
      </form>
      {toast && (
        <Toast
          success={toast.success}
          message={toast.message}
          onClose={() => setToast(null)}
        />
      )}
    </div>
  );
}

export default ProfilePage;
