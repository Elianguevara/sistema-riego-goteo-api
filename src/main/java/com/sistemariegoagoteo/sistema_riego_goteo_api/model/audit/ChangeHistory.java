package com.sistemariegoagoteo.sistema_riego_goteo_api.model.audit; 

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "change_history")
public class ChangeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") 
    private User user;

    @Column(name = "action_type", length = 20)
    private String actionType;

    @Column(name = "affected_table", length = 100) 
    private String affectedTable;

    @Column(name = "changed_field", length = 100)
    private String changedField;

    @Column(name = "old_value", length = 255) 
    private String oldValue;

    @Column(name = "new_value", length = 255) 
    private String newValue;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "change_datetime")
    private Date changeDatetime;
}