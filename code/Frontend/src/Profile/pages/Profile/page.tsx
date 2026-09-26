import { useEffect, useState, type ChangeEvent, type FormEvent } from "react";
import { FiCamera } from "react-icons/fi";
import Button from "../../../components/Button";
import Input from "../../../components/Input";
import PageHeader from "../../../components/PageHeader";
import { ErrorState, LoadingState } from "../../../components/ViewState";
import { useWorkspace } from "../../../Workspace/useWorkspace";
import type { ChangePasswordInput, Profile } from "../../../types/profile";
import type { ResourceInput } from "../../../types/workspace";
import { getErrorMessage } from "../../../api/apiError";
import { initials } from "../../../utils/formatters";
import {
  removeStoredProfileImage,
  saveStoredProfileImage,
} from "../../../services/profile";
import {
  loadThaiAddressData,
  type ThaiProvince,
} from "../../../services/thaiAddress";

const MAX_PROFILE_IMAGE_SIZE = 2 * 1024 * 1024;

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
};

function ProfilePage() {
  const { data, loading, error, refresh, save } = useWorkspace();
  const [draft, setDraft] = useState<
    Profile | ResourceInput<"profiles"> | null
  >(null);
  const [message, setMessage] = useState("");
  const [saving, setSaving] = useState(false);
  const [provinces, setProvinces] = useState<ThaiProvince[]>([]);
  const [addressLoading, setAddressLoading] = useState(true);
  const [addressError, setAddressError] = useState("");
  const [passwordForm, setPasswordForm] = useState<ChangePasswordInput>({
    current_password: "",
    new_password: "",
    confirm_password: "",
  });
  const [passwordMessage, setPasswordMessage] = useState("");

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

  const updateFields = (values: Partial<ResourceInput<"profiles">>) => {
    setDraft((current) => ({
      ...(current || data.profiles[0] || emptyProfile),
      ...values,
    }));
    setMessage("");
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
    setMessage("");
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

  const updateImage = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) return;
    if (!file.type.startsWith("image/")) {
      setMessage("กรุณาเลือกไฟล์รูปภาพเท่านั้น");
      return;
    }
    if (file.size > MAX_PROFILE_IMAGE_SIZE) {
      setMessage("รูปโปรไฟล์ต้องมีขนาดไม่เกิน 2 MB");
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      const imageUrl = reader.result;
      if (typeof imageUrl !== "string") {
        setMessage("ไม่สามารถอ่านไฟล์รูปภาพได้");
        return;
      }
      setDraft((current) => ({
        ...(current || data.profiles[0] || emptyProfile),
        logo_url: imageUrl,
      }));
      setMessage("เลือกรูปโปรไฟล์แล้ว กดบันทึกเพื่อยืนยัน");
    };
    reader.onerror = () => setMessage("ไม่สามารถอ่านไฟล์รูปภาพได้");
    reader.readAsDataURL(file);
  };

  const removeImage = () => {
    setDraft((current) => ({
      ...(current || data.profiles[0] || emptyProfile),
      logo_url: "",
    }));
    setMessage("ลบรูปโปรไฟล์แล้ว กดบันทึกเพื่อยืนยัน");
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSaving(true);
    if (profile.id) {
      if (profile.logo_url)
        saveStoredProfileImage(profile.id, profile.logo_url);
      else removeStoredProfileImage(profile.id);
    }
    try {
      await save("profiles", {
        ...profile,
        default_tax_rate: Number(profile.default_tax_rate) || 0,
        default_hourly_rate: Number(profile.default_hourly_rate) || 0,
      });
      setDraft(null);
      setMessage("บันทึกโปรไฟล์เรียบร้อยแล้ว");
    } catch (err) {
      setDraft(null);
      await refresh();
      if (
        err instanceof Error &&
        err.message.includes("profiles ยังไม่มี endpoint")
      ) {
        setMessage(
          "บันทึกรูปโปรไฟล์เรียบร้อยแล้ว รูปจะถูกใช้ในอุปกรณ์นี้จนกว่าจะมี API สำหรับโปรไฟล์",
        );
      } else {
        setMessage(getErrorMessage(err, "ไม่สามารถบันทึกโปรไฟล์ได้"));
      }
    } finally {
      setSaving(false);
    }
  };

  const updatePassword = (event: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;
    setPasswordForm((current) => ({ ...current, [name]: value }));
    setPasswordMessage("");
  };

  const submitPassword = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (passwordForm.new_password.length < 8) {
      setPasswordMessage("รหัสผ่านใหม่ต้องมีอย่างน้อย 8 ตัวอักษร");
      return;
    }
    if (passwordForm.new_password !== passwordForm.confirm_password) {
      setPasswordMessage("ยืนยันรหัสผ่านใหม่ไม่ตรงกัน");
      return;
    }
    if (passwordForm.current_password === passwordForm.new_password) {
      setPasswordMessage("รหัสผ่านใหม่ต้องไม่ซ้ำกับรหัสผ่านเดิม");
      return;
    }
    setPasswordMessage(
      "ระบบ backend ยังไม่มี API สำหรับเปลี่ยนรหัสผ่าน จึงยังไม่สามารถบันทึกการเปลี่ยนแปลงได้",
    );
  };

  return (
    <div className="page-view profile-page">
      <PageHeader
        eyebrow="บัญชีของฉัน"
        title="ข้อมูลส่วนตัว"
        description="ข้อมูลที่ใช้แสดงในบัญชีและเอกสารของคุณ"
      />

      <form className="profile-layout" onSubmit={submit}>
        <section className="panel profile-account-card">
          <div className="profile-form-identity">
            <div className="profile-photo-area">
              <label
                className="profile-photo-control"
                htmlFor="profile-image"
                aria-label="อัปโหลดรูปโปรไฟล์"
              >
                <span className="profile-photo-preview">
                  {profile.logo_url ? (
                    <img src={profile.logo_url} alt="รูปโปรไฟล์" />
                  ) : (
                    <span>{initials(profile.full_name)}</span>
                  )}
                </span>
                <span className="profile-camera-icon" aria-hidden="true">
                  <FiCamera />
                </span>
              </label>
              <input
                className="profile-image-input"
                id="profile-image"
                type="file"
                accept="image/png,image/jpeg,image/webp"
                onChange={updateImage}
              />
              <div className="profile-photo-actions">
                {profile.logo_url && (
                  <Button
                    variant="text"
                    className="profile-remove-button"
                    type="button"
                    onClick={removeImage}
                  >
                    ลบรูป
                  </Button>
                )}
              </div>
              <small>JPG, PNG หรือ WEBP ขนาดไม่เกิน 2 MB</small>
            </div>
            <div className="profile-display-name-field form-field">
              <label htmlFor="profile-display-name">ชื่อที่แสดง</label>
              <Input
                id="profile-display-name"
                name="display_name"
                value={profile.display_name || ""}
                onChange={update}
                required
              />
              <small>ชื่อนี้จะแสดงบนหน้าเว็บและเมนูบัญชี</small>
            </div>
          </div>
        </section>

        <section className="panel profile-form-card">
          <div className="form-grid profile-contact-grid">
            <div className="form-field">
              <label htmlFor="profile-first-name">ชื่อ</label>
              <Input
                id="profile-first-name"
                name="first_name"
                value={profile.first_name || ""}
                onChange={update}
                required
              />
            </div>
            <div className="form-field">
              <label htmlFor="profile-last-name">นามสกุล</label>
              <Input
                id="profile-last-name"
                name="last_name"
                value={profile.last_name || ""}
                onChange={update}
                required
              />
            </div>
            <div className="form-field">
              <label htmlFor="profile-email">อีเมลบัญชี</label>
              <Input
                id="profile-email"
                name="email"
                type="email"
                value={profile.email || ""}
                disabled
              />
              <small>อีเมลนี้ใช้สำหรับเข้าสู่ระบบ</small>
            </div>
            <div className="form-field">
              <label htmlFor="profile-phone">เบอร์โทรศัพท์</label>
              <Input
                id="profile-phone"
                name="phone"
                value={profile.phone || ""}
                onChange={update}
              />
            </div>
          </div>

          <div className="profile-divider" />
          <p className="profile-section-label">ที่อยู่จัดส่งเอกสาร</p>
          {addressError && <p className="form-message error">{addressError}</p>}

          <div className="form-grid profile-address-grid">
            <div className="form-field full">
              <label htmlFor="profile-address">ที่อยู่</label>
              <Input
                id="profile-address"
                name="address"
                value={profile.address || ""}
                onChange={update}
              />
            </div>
            <div className="form-field">
              <label htmlFor="profile-province">จังหวัด</label>
              <select
                id="profile-province"
                value={profile.province || ""}
                onChange={updateProvince}
                disabled={addressLoading || Boolean(addressError)}
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
            </div>
            <div className="form-field">
              <label htmlFor="profile-district">อำเภอ / เขต</label>
              <select
                id="profile-district"
                value={profile.district || ""}
                onChange={updateDistrict}
                disabled={!selectedProvince}
              >
                <option value="">เลือกอำเภอ / เขต</option>
                {districts.map((district) => (
                  <option key={district.id} value={district.name_th}>
                    {district.name_th}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-field">
              <label htmlFor="profile-sub-district">ตำบล / แขวง</label>
              <select
                id="profile-sub-district"
                value={profile.sub_district || ""}
                onChange={updateSubDistrict}
                disabled={!selectedDistrict}
              >
                <option value="">เลือกตำบล / แขวง</option>
                {subDistricts.map((subDistrict) => (
                  <option key={subDistrict.id} value={subDistrict.name_th}>
                    {subDistrict.name_th}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-field">
              <label htmlFor="profile-postal-code">รหัสไปรษณีย์</label>
              <select
                id="profile-postal-code"
                name="postal_code"
                value={profile.postal_code || ""}
                disabled={!selectedSubDistrict}
                onChange={update}
              >
                <option value="">เลือกตำบลก่อน</option>
                {selectedSubDistrict && (
                  <option value={String(selectedSubDistrict.zip_code)}>
                    {selectedSubDistrict.zip_code}
                  </option>
                )}
              </select>
            </div>
            <div className="form-field">
              <label htmlFor="profile-tax">เลขประจำตัวผู้เสียภาษี</label>
              <Input
                id="profile-tax"
                name="tax_id"
                value={profile.tax_id || ""}
                onChange={update}
              />
            </div>
          </div>

          {message && (
            <p
              className={`form-message ${message.includes("เรียบร้อย") || message.includes("เลือกรูป") || message.includes("ลบรูป") ? "success" : "error"}`}
            >
              {message}
            </p>
          )}

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
              disabled={saving}
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
            <label htmlFor="current-password">รหัสผ่านเดิม</label>
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
            <label htmlFor="new-password">รหัสผ่านใหม่</label>
            <Input
              id="new-password"
              name="new_password"
              type="password"
              value={passwordForm.new_password}
              onChange={updatePassword}
              autoComplete="new-password"
              minLength={8}
              required
            />
            <small>ต้องมีอย่างน้อย 8 ตัวอักษร</small>
          </div>
          <div className="form-field">
            <label htmlFor="confirm-password">ยืนยันรหัสผ่านใหม่</label>
            <Input
              id="confirm-password"
              name="confirm_password"
              type="password"
              value={passwordForm.confirm_password}
              onChange={updatePassword}
              autoComplete="new-password"
              minLength={8}
              required
            />
          </div>
        </div>
        {passwordMessage && (
          <p className="form-message error">{passwordMessage}</p>
        )}
        <div className="profile-form-actions">
          <Button variant="primary" type="submit">
            เปลี่ยนรหัสผ่าน
          </Button>
        </div>
      </form>
    </div>
  );
}

export default ProfilePage;
