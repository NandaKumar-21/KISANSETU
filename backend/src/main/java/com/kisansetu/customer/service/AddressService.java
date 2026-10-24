package com.kisansetu.customer.service;

import com.kisansetu.common.exception.ApiException;
import com.kisansetu.customer.dto.AddressRequest;
import com.kisansetu.customer.entity.CustomerAddress;
import com.kisansetu.customer.repository.CustomerAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Customer address book.
 */
@Service
@RequiredArgsConstructor
public class AddressService {

    private final CustomerAddressRepository addressRepository;

    @Transactional(readOnly = true)
    public List<CustomerAddress> getAddresses(UUID customerId) {
        return addressRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Transactional
    public CustomerAddress addAddress(UUID customerId, AddressRequest request) {
        CustomerAddress address = new CustomerAddress();
        applyRequest(address, request);
        address.setCustomerId(customerId);
        addressRepository.save(address);
        if (address.isDefault()) {
            clearOtherDefaults(customerId, address.getId());
        }
        return address;
    }

    @Transactional
    public CustomerAddress updateAddress(UUID customerId, UUID addressId, AddressRequest request) {
        CustomerAddress address = getOwnedAddress(customerId, addressId);
        applyRequest(address, request);
        addressRepository.save(address);
        if (address.isDefault()) {
            clearOtherDefaults(customerId, address.getId());
        }
        return address;
    }

    @Transactional
    public void deleteAddress(UUID customerId, UUID addressId) {
        addressRepository.delete(getOwnedAddress(customerId, addressId));
    }

    @Transactional
    public CustomerAddress setDefault(UUID customerId, UUID addressId) {
        CustomerAddress address = getOwnedAddress(customerId, addressId);
        clearOtherDefaults(customerId, addressId);
        address.setDefault(true);
        addressRepository.save(address);
        return address;
    }

    private CustomerAddress getOwnedAddress(UUID customerId, UUID addressId) {
        CustomerAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> ApiException.notFound("Address not found"));
        if (!address.getCustomerId().equals(customerId)) {
            throw ApiException.forbidden("This address does not belong to you");
        }
        return address;
    }

    private void clearOtherDefaults(UUID customerId, UUID exceptId) {
        addressRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .forEach(a -> {
                    if (!a.getId().equals(exceptId) && a.isDefault()) {
                        a.setDefault(false);
                        addressRepository.save(a);
                    }
                });
    }

    private void applyRequest(CustomerAddress address, AddressRequest request) {
        String line2 = request.addressLine2() == null ? "" : ", " + request.addressLine2().trim();
        address.setLabel(request.addressLine1().trim());
        address.setAddressLine(request.addressLine1().trim() + line2);
        address.setCity(request.city());
        address.setState(request.state());
        address.setPincode(request.pincode());
        address.setPhone(request.phone());
        address.setLatitude(request.latitude());
        address.setLongitude(request.longitude());
        if (request.isDefault() != null) {
            address.setDefault(request.isDefault());
        }
    }
}