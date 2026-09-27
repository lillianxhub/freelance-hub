import { useState, type ChangeEvent, type FormEvent } from "react";
import PageHeader from "../../../components/PageHeader";
import { ErrorState, LoadingState } from "../../../components/ViewState";
import { useWorkspace } from "../../../Workspace/useWorkspace";
import type { Profile } from "../../../types/profile";
import type { ResourceInput } from "../../../types/workspace";
import { getErrorMessage } from "../../../api/apiError";
import { initials } from "../../../utils/formatters";

const emptyProfile: ResourceInput<"profiles"> = {
  full_name: "",
  email: "",
  phone: "",
  address: "",
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

function SettingsPage() {
  const { data, loading, error, refresh, save } = useWorkspace();
  const [draft, setDraft] = useState<
    Profile | ResourceInput<"profiles"> | null
  >(null);
  const [message, setMessage] = useState("");
  const [saving, setSaving] = useState(false);

  if (loading) return <LoadingState label="กำลังโหลดการตั้งค่า..." />;
  if (error) return <ErrorState message={error} onRetry={refresh} />;

  const profile = draft || data.profiles[0] || emptyProfile;
  const update = (
    event: ChangeEvent<
      HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement
    >,
  ) => {
    const { name, value } = event.target;
    setDraft((current) => ({
      ...(current || data.profiles[0]),
      [name]: value,
    }));
    setMessage("");
  };
  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSaving(true);
    try {
      await save("profiles", {
        ...profile,
        default_tax_rate: Number(profile.default_tax_rate) || 0,
        default_hourly_rate: Number(profile.default_hourly_rate) || 0,
      });
      setDraft(null);
      setMessage("บันทึกการตั้งค่าเรียบร้อยแล้ว");
    } catch (err) {
      setMessage(getErrorMessage(err, "ไม่สามารถบันทึกการตั้งค่าได้"));
    } finally {
      setSaving(false);
    }
  };
  return (
    <div className="page-view">
      <PageHeader
        eyebrow="จัดการ / Settings"
        title="ตั้งค่า"
        description="Settingsข้อมูลธุรกิจ ค่าเริ่มต้น และDescriptionที่ใช้ใน Invoices"
      />
      <form className="settings-layout" onSubmit={submit}>
        <aside className="panel settings-profile-card">
          <div className="profile-logo-preview">
            {profile.logo_url ? (
              <img src={profile.logo_url} alt="โลโก้ธุรกิจ" />
            ) : (
              <span>{initials(profile.full_name)}</span>
            )}
          </div>
          <h2>{profile.full_name || "ฟรีแลนซ์"}</h2>
          <p>{profile.email}</p>
          <span className="mode-badge">ข้อมูลจากระบบ</span>
          <div className="settings-note">
            <strong>ข้อมูลชุดเดียวกับ ใบแจ้งหนี้</strong>
            <p>
              ชื่อ ที่อยู่ ภาษี และบัญชีธนาคารจะถูกคัดลอกเป็น คัดลอกข้อมูล
              ตอนสร้าง ใบแจ้งหนี้
            </p>
          </div>
        </aside>
        <div className="section-stack">
          {message && (
            <p
              className={`form-message ${message.includes("เรียบร้อย") || message.includes("เริ่มต้น") ? "success" : "error"}`}
            >
              {message}
            </p>
          )}
          <section className="panel">
            <div className="panel-heading">
              <div>
                <h2>โปรไฟล์และข้อมูลติดต่อ</h2>
                <p>ข้อมูลผู้ขายที่ลูกค้าจะเห็น</p>
              </div>
            </div>
            <div className="form-grid">
              <div className="form-field">
                <label htmlFor="profile-name">ชื่อ-นามสกุล / ชื่อธุรกิจ</label>
                <input
                  id="profile-name"
                  name="full_name"
                  value={profile.full_name || ""}
                  onChange={update}
                  required
                />
              </div>
              <div className="form-field">
                <label htmlFor="profile-email">อีเมลบัญชี</label>
                <input
                  id="profile-email"
                  name="email"
                  type="email"
                  value={profile.email || ""}
                  onChange={update}
                  disabled
                />
                <small>อีเมลบัญชีใช้สำหรับเข้าสู่ระบบ</small>
              </div>
              <div className="form-field">
                <label htmlFor="profile-phone">โทรศัพท์</label>
                <input
                  id="profile-phone"
                  name="phone"
                  value={profile.phone || ""}
                  onChange={update}
                />
              </div>
              <div className="form-field">
                <label htmlFor="profile-tax">เลขประจำตัวผู้เสียภาษี</label>
                <input
                  id="profile-tax"
                  name="tax_id"
                  value={profile.tax_id || ""}
                  onChange={update}
                />
              </div>
              <div className="form-field full">
                <label htmlFor="profile-address">ที่อยู่</label>
                <textarea
                  id="profile-address"
                  name="address"
                  value={profile.address || ""}
                  onChange={update}
                />
              </div>
              <div className="form-field full">
                <label htmlFor="profile-logo">ลิงก์โลโก้</label>
                <input
                  id="profile-logo"
                  name="logo_url"
                  type="url"
                  value={profile.logo_url || ""}
                  onChange={update}
                  placeholder="https://..."
                />
                <small>ใช้ URL รูปภาพที่เปิดดูได้สาธารณะ</small>
              </div>
            </div>
          </section>
          {/* 
          <section className="panel"><div className="panel-heading"><div><h2>ข้อมูลรับชำระ</h2><p>แสดงท้าย ใบแจ้งหนี้ เพื่อให้ลูกค้าชำระได้สะดวก</p></div></div><div className="form-grid">
            <div className="form-field"><label htmlFor="bank-name">ธนาคาร</label><input id="bank-name" name="bank_name" value={profile.bank_name || ''} onChange={update} /></div>
            <div className="form-field"><label htmlFor="bank-owner">ชื่อบัญชี</label><input id="bank-owner" name="bank_account_name" value={profile.bank_account_name || ''} onChange={update} /></div>
            <div className="form-field full"><label htmlFor="bank-number">เลขที่บัญชี</label><input id="bank-number" name="bank_account_number" value={profile.bank_account_number || ''} onChange={update} /></div>
          </div></section> */}

          <section className="panel">
            <div className="panel-heading">
              <div>
                <h2>ค่าเริ่มต้นของ พื้นที่ทำงาน</h2>
                <p>ใช้เป็นค่าตั้งต้นเมื่อสร้างโปรเจกต์ เวลา และ ใบแจ้งหนี้</p>
              </div>
            </div>
            <div className="form-grid">
              <div className="form-field">
                <label htmlFor="settings-timezone">เขตเวลา</label>
                <select
                  id="settings-timezone"
                  name="timezone"
                  value={profile.timezone || "Asia/Bangkok"}
                  onChange={update}
                >
                  <option value="Asia/Bangkok">Asia/Bangkok (UTC+7)</option>
                  <option value="Asia/Singapore">Asia/Singapore (UTC+8)</option>
                  <option value="UTC">UTC</option>
                </select>
              </div>
              <div className="form-field">
                <label htmlFor="settings-currency">สกุลเงินหลัก</label>
                <select
                  id="settings-currency"
                  name="currency"
                  value={profile.currency || "THB"}
                  onChange={update}
                >
                  <option value="THB">THB — บาทไทย</option>
                  <option value="USD">USD — ดอลลาร์สหรัฐ</option>
                  <option value="EUR">EUR — ยูโร</option>
                  <option value="SGD">SGD — ดอลลาร์สิงคโปร์</option>
                </select>
              </div>
              <div className="form-field">
                <label htmlFor="settings-date">รูปแบบวันที่</label>
                <select
                  id="settings-date"
                  name="date_format"
                  value={profile.date_format || "DD/MM/YYYY"}
                  onChange={update}
                >
                  <option value="DD/MM/YYYY">DD/MM/YYYY</option>
                  <option value="MM/DD/YYYY">MM/DD/YYYY</option>
                  <option value="YYYY-MM-DD">YYYY-MM-DD</option>
                </select>
              </div>
              <div className="form-field">
                <label htmlFor="settings-rate">อัตรารายชั่วโมงเริ่มต้น</label>
                <input
                  id="settings-rate"
                  name="default_hourly_rate"
                  type="number"
                  min="0"
                  step="0.01"
                  value={profile.default_hourly_rate || 0}
                  onChange={update}
                />
              </div>
              <div className="form-field">
                <label htmlFor="settings-tax-rate">ภาษีเริ่มต้น (%)</label>
                <input
                  id="settings-tax-rate"
                  name="default_tax_rate"
                  type="number"
                  min="0"
                  max="100"
                  step="0.01"
                  value={profile.default_tax_rate || 0}
                  onChange={update}
                />
              </div>
            </div>
          </section>

          <div className="settings-actions">
            <div />
            <button
              className="button button-primary"
              type="submit"
              disabled={saving}
            >
              {saving ? "กำลังบันทึก..." : "SaveการSettings"}
            </button>
          </div>
        </div>
      </form>
    </div>
  );
}

export default SettingsPage;
