package org.admin.gateway.entity.user;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.relational.core.mapping.Table;

@Data
@ToString
@Builder
@Table(name= "tb_user")
public class UserDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userCode;

    private String userId;

    private String userPw;

    private String userName;

    private String email;

    private String roleGroup;
}
