package com.invoiceapp.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@SuppressWarnings("JpaDataSourceORMInspection")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity

//client entity
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 150)
    private String name;
    private String email;
    private String phone;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
