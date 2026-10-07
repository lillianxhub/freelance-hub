import { Card, CardDescription, CardHeader, CardTitle } from '../../../components/ui/card'
import { NativeSelect } from '../../../components/ui/native-select'
import { useState, type ChangeEvent, type FormEvent } from "react";
import { Button } from "../../../components/ui/button";
import FormLabel from "../../../components/FormLabel";
import { Input } from "../../../components/ui/input";
import { Textarea } from '../../../components/ui/textarea'
import PasswordInput from "../../../components/PasswordInput";
import PageHeader from "../../../components/PageHeader";
import { ErrorState, LoadingState } from "../../../components/ViewState";
import { useProfile } from "../../useProfile";
import type { ChangePasswordInput, Profile, ProfileFieldErrors } from "../../../types/profile";
import type { ProfileInput } from "../../../types/profile";
import type { NotificationMessage } from "../../../types/notification";
import { toast } from 'sonner';
import type { FieldErrorProps } from "../../../types/ui";
import { getErrorMessage } from "../../../api/apiError";
import { changePassword } from "../../../services/profile";
import { validateProfileFields } from "../../profile.validators";
import { useThaiAddress } from '../../useThaiAddress'

const emptyProfile: ProfileInput = {
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
  default_tax_rate: 0,
  default_hourly_rate: 0,
  bio: "",
};

const editableProfileFields: Array<keyof ProfileInput> = [
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
  return message ? <p id={id} className="mt-1 text-xs font-semibold text-destructive">{message}</p> : null;
}

function ProfilePage() {
  const { data, loading, error, refresh, updateProfile } = useProfile();
  const [draft, setDraft] = useState<
    Profile | ProfileInput | null
  >(null);
  const showToast = ({ success, message }: NotificationMessage) => {
    toast[success ? 'success' : 'error'](message);
  };
  const [saving, setSaving] = useState(false);
  const { provinces, loading: addressLoading, error: addressError } = useThaiAddress()
  const [profileErrors, setProfileErrors] = useState<ProfileFieldErrors>({});
  const [passwordForm, setPasswordForm] = useState<ChangePasswordInput>({
    current_password: "",
    new_password: "",
    confirm_password: "",
  });
  const [passwordSaving, setPasswordSaving] = useState(false);

  if (loading) return <LoadingState label="กำลังโหลดโปรไฟล์..." />;
  if (error) return <ErrorState message={error} onRetry={refresh} />;

  const profile = draft || data || emptyProfile;
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
  const savedProfile = data || emptyProfile;
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

  const updateFields = (values: Partial<ProfileInput>) => {
    setDraft((current) => ({
      ...(current || data || emptyProfile),
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
      ...(current || data || emptyProfile),
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
      showToast({
        success: response.success,
        message: response.message || "บันทึกโปรไฟล์เรียบร้อยแล้ว",
      });
    } catch (err) {
      showToast({
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
      showToast({
        success: false,
        message: "รหัสผ่านใหม่ต้องมีอย่างน้อย 8 ตัวอักษร",
      });
      return;
    }
    if (passwordForm.new_password !== passwordForm.confirm_password) {
      showToast({ success: false, message: "ยืนยันรหัสผ่านใหม่ไม่ตรงกัน" });
      return;
    }
    if (passwordForm.current_password === passwordForm.new_password) {
      showToast({
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
      showToast({
        success: response.success,
        message: response.message || "เปลี่ยนรหัสผ่านเรียบร้อยแล้ว",
      });
    } catch (passwordError) {
      showToast({
        success: false,
        message: getErrorMessage(passwordError, "ไม่สามารถเปลี่ยนรหัสผ่านได้"),
      });
    } finally {
      setPasswordSaving(false);
    }
  };

  return (
    <div className="mx-auto w-full max-w-screen-2xl">
      <PageHeader
        title="ข้อมูลส่วนตัว"
      />

      <form className="grid gap-4" onSubmit={submit} noValidate>
        <Card asChild><section className="p-6">
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="col-span-full flex flex-col gap-1.5">
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
              <small className="text-xs text-muted-foreground">ชื่อนี้จะแสดงบนหน้าเว็บและเมนูบัญชี</small>
            </div>
            <div className="flex flex-col gap-1.5">
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
            <div className="flex flex-col gap-1.5">
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
            <div className="flex flex-col gap-1.5">
              <FormLabel htmlFor="profile-email">อีเมลบัญชี</FormLabel>
              <Input
                id="profile-email"
                name="email"
                type="email"
                value={profile.email || ""}
                disabled
                aria-disabled="true"
              />
              <small className="text-xs text-muted-foreground">อีเมลนี้ใช้สำหรับเข้าสู่ระบบ</small>
            </div>
            <div className="flex flex-col gap-1.5">
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
              <small className="text-xs text-muted-foreground">กรอกตัวเลขได้ไม่เกิน 10 หลัก</small>
            </div>
            <div className="flex flex-col gap-1.5">
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
              <small className="text-xs text-muted-foreground">กรอกตัวเลขได้ไม่เกิน 13 หลัก</small>
            </div>
            {/* <div className="col-span-full flex flex-col gap-1.5">
              <label htmlFor="profile-bio">แนะนำตัว</label>
              <Textarea
                id="profile-bio"
                name="bio"
                value={profile.bio || ""}
                onChange={update}
                maxLength={1000}
                placeholder="เขียนแนะนำตัวหรือรายละเอียดการทำงานโดยย่อ"
              />
            </div> */}
          </div>

          <div className="my-6 border-t border-border" />
          <p className="mb-3 text-sm font-semibold tracking-wide text-text-secondary">ที่อยู่</p>
          {addressError && <p className="rounded-lg bg-red-soft px-3 py-2 text-sm text-destructive">{addressError}</p>}

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="col-span-full flex flex-col gap-1.5">
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
            <div className="flex flex-col gap-1.5">
              <FormLabel htmlFor="profile-province" required>จังหวัด</FormLabel>
              <NativeSelect
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
              </NativeSelect>
              <FieldError id="profile-province-error" message={profileErrors.province} />
            </div>
            <div className="flex flex-col gap-1.5">
              <FormLabel htmlFor="profile-district" required>อำเภอ / เขต</FormLabel>
              <NativeSelect
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
              </NativeSelect>
              <FieldError id="profile-district-error" message={profileErrors.district} />
            </div>
            <div className="flex flex-col gap-1.5">
              <FormLabel htmlFor="profile-sub-district" required>ตำบล / แขวง</FormLabel>
              <NativeSelect
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
              </NativeSelect>
              <FieldError id="profile-sub-district-error" message={profileErrors.sub_district} />
            </div>
            <div className="flex flex-col gap-1.5">
              <FormLabel htmlFor="profile-postal-code" required>รหัสไปรษณีย์</FormLabel>
              <NativeSelect
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
              </NativeSelect>
              <FieldError id="profile-postal-code-error" message={profileErrors.postal_code} />
            </div>
          </div>

          <div className="mt-6 flex flex-col gap-2 border-t border-border pt-6 sm:flex-row sm:justify-end">
            <Button
              variant="outline"
              className="min-h-10 w-full sm:w-auto"
              type="button"
              onClick={() => setDraft(null)}
            >
              ยกเลิก
            </Button>
            <Button
              className="min-h-10 w-full sm:w-auto"
              type="submit"
              disabled={saving || !hasProfileChanges}
            >
              {saving ? "กำลังบันทึก..." : "บันทึกการเปลี่ยนแปลง"}
            </Button>
          </div>
        </section></Card>
      </form>

      <Card asChild><form className="mt-4 p-6" onSubmit={submitPassword}>
        <CardHeader className="mb-6 p-0">
          <CardTitle>เปลี่ยนรหัสผ่าน</CardTitle>
          <CardDescription>ใช้รหัสผ่านเดิมเพื่อกำหนดรหัสผ่านใหม่สำหรับเข้าสู่ระบบ</CardDescription>
        </CardHeader>
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="flex flex-col gap-1.5">
            <FormLabel htmlFor="current-password" required>รหัสผ่านเดิม</FormLabel>
            <PasswordInput
              id="current-password"
              name="current_password"
              value={passwordForm.current_password}
              onChange={updatePassword}
              autoComplete="current-password"
              required
            />
          </div>
          <div className="flex flex-col gap-1.5">
            <FormLabel htmlFor="new-password" required>รหัสผ่านใหม่</FormLabel>
            <PasswordInput
              id="new-password"
              name="new_password"
              value={passwordForm.new_password}
              onChange={updatePassword}
              autoComplete="new-password"
              minLength={8}
              aria-invalid={passwordTooShort || passwordUnchanged}
              required
            />
            {passwordTooShort ? (
              <small className="mt-1 text-xs font-semibold text-destructive">รหัสผ่านใหม่ต้องมีอย่างน้อย 8 ตัวอักษร</small>
            ) : (
              <small className="text-xs text-muted-foreground">ต้องมีอย่างน้อย 8 ตัวอักษร</small>
            )}
            {passwordUnchanged && (
              <small className="mt-1 text-xs font-semibold text-destructive">รหัสผ่านใหม่ต้องไม่ซ้ำกับรหัสผ่านเดิม</small>
            )}
          </div>
          <div className="flex flex-col gap-1.5">
            <FormLabel htmlFor="confirm-password" required>ยืนยันรหัสผ่านใหม่</FormLabel>
            <PasswordInput
              id="confirm-password"
              name="confirm_password"
              value={passwordForm.confirm_password}
              onChange={updatePassword}
              autoComplete="new-password"
              minLength={8}
              aria-invalid={passwordMismatch}
              required
            />
            {passwordMismatch && (
              <small className="mt-1 text-xs font-semibold text-destructive">ยืนยันรหัสผ่านใหม่ไม่ตรงกัน</small>
            )}
          </div>
        </div>
        <div className="mt-6 flex flex-col gap-2 border-t border-border pt-6 sm:flex-row sm:justify-end">
          <Button
            className="min-h-10 w-full sm:w-auto"
            type="submit"
            disabled={passwordSaving || passwordFormInvalid}
          >
            {passwordSaving ? "กำลังเปลี่ยนรหัสผ่าน..." : "เปลี่ยนรหัสผ่าน"}
          </Button>
        </div>
      </form></Card>
    </div>
  );
}

export default ProfilePage;
