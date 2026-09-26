package th.ac.kku.freelance_hub.mapper;

import org.springframework.stereotype.Component;

import th.ac.kku.freelance_hub.domain.entity.Address;

/** Creates and updates normalized address entities from flat API fields. */
@Component
public class AddressMapper {

    public Address toEntity(
        String address,
        String subdistrict,
        String district,
        String province,
        String postalCode
    ) {
        if (!hasAnyNonBlankValue(address, subdistrict, district, province, postalCode)) {
            return null;
        }
        return new Address(address, subdistrict, district, province, postalCode);
    }

    public boolean hasAnyValue(
        String address,
        String subdistrict,
        String district,
        String province,
        String postalCode
    ) {
        return address != null
            || subdistrict != null
            || district != null
            || province != null
            || postalCode != null;
    }

    private boolean hasAnyNonBlankValue(
        String address,
        String subdistrict,
        String district,
        String province,
        String postalCode
    ) {
        return isNonBlank(address)
            || isNonBlank(subdistrict)
            || isNonBlank(district)
            || isNonBlank(province)
            || isNonBlank(postalCode);
    }

    private boolean isNonBlank(String value) {
        return value != null && !value.isBlank();
    }

    public void update(
        Address target,
        String address,
        String subdistrict,
        String district,
        String province,
        String postalCode
    ) {
        target.update(
            address == null ? target.getAddress() : address,
            subdistrict == null ? target.getSubdistrict() : subdistrict,
            district == null ? target.getDistrict() : district,
            province == null ? target.getProvince() : province,
            postalCode == null ? target.getPostalCode() : postalCode
        );
    }
}
