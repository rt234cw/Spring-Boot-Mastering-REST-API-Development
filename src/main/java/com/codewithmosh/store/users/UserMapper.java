package com.codewithmosh.store.users;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;


@Mapper(
        componentModel = "spring",
        builder = @org.mapstruct.Builder(disableBuilder = true)
)
public interface UserMapper {
    UserDto toDto(User user);

    User toEntity(RegisterUserRequest registerUserRequest);

    void update(UpdateUserRequest updateUserRequest, @MappingTarget User user);


}
