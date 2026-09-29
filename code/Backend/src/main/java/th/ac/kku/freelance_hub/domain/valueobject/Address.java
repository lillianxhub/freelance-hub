package th.ac.kku.freelance_hub.domain.valueobject;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Address value stored in the owning entity's table. */
@Embeddable
public class Address {

    @Column(name = "address", columnDefinition = "text")
    private String address;

    @Column(name = "subdistrict", length = 100)
    private String subdistrict;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "province", length = 100)
    private String province;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    protected Address() {
        // Required by JPA.
    }

    public Address(String address, String subdistrict, String district, String province, String postalCode) {
        this.address = trimToNull(address);
        this.subdistrict = trimToNull(subdistrict);
        this.district = trimToNull(district);
        this.province = trimToNull(province);
        this.postalCode = trimToNull(postalCode);
    }

    /** Returns null when every optional address field is empty. */
    public static Address ofNullable(
            String address, String subdistrict, String district, String province, String postalCode) {
        Address value = new Address(address, subdistrict, district, province, postalCode);
        return value.isEmpty() ? null : value;
    }

    private boolean isEmpty() {
        return address == null && subdistrict == null && district == null
                && province == null && postalCode == null;
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public String getAddress() { return address; }
    public String getSubdistrict() { return subdistrict; }
    public String getDistrict() { return district; }
    public String getProvince() { return province; }
    public String getPostalCode() { return postalCode; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Address that)) return false;
        return Objects.equals(address, that.address)
                && Objects.equals(subdistrict, that.subdistrict)
                && Objects.equals(district, that.district)
                && Objects.equals(province, that.province)
                && Objects.equals(postalCode, that.postalCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, subdistrict, district, province, postalCode);
    }
}
