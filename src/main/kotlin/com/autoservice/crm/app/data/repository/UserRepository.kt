package com.autoservice.crm.data.repository

import com.autoservice.crm.data.entity.Users
import com.autoservice.crm.domain.model.UserDTO
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.dao.id.EntityID

class UserRepository {

    fun findAll(): List<UserDTO> = transaction {
        Users.selectAll().map { row ->
            UserDTO(
                id = row[Users.id].value,
                fullName = row[Users.fullName],
                username = row[Users.username],
                passwordHash = row[Users.passwordHash],
                roleName = row[Users.roleName]
            )
        }
    }

    fun findById(id: Long): UserDTO? = transaction {
        Users.selectAll()
            .where { Users.id eq EntityID(id, Users) }
            .map { row ->
                UserDTO(
                    id = row[Users.id].value,
                    fullName = row[Users.fullName],
                    username = row[Users.username],
                    passwordHash = row[Users.passwordHash],
                    roleName = row[Users.roleName]
                )
            }
            .singleOrNull()
    }

    fun findByUsername(username: String): UserDTO? = transaction {
        Users.selectAll()
            .where { Users.username eq username }
            .map { row ->
                UserDTO(
                    id = row[Users.id].value,
                    fullName = row[Users.fullName],
                    username = row[Users.username],
                    passwordHash = row[Users.passwordHash],
                    roleName = row[Users.roleName]
                )
            }
            .singleOrNull()
    }

    fun findAllMechanics(): List<UserDTO> = transaction {
        Users.selectAll()
            .where { Users.roleName eq "Механик" }
            .map { row ->
                UserDTO(
                    id = row[Users.id].value,
                    fullName = row[Users.fullName],
                    username = row[Users.username],
                    passwordHash = row[Users.passwordHash],
                    roleName = row[Users.roleName]
                )
            }
    }
}