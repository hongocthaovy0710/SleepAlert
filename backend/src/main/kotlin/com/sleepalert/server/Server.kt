package com.sleepalert.server

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

// Bảng users
object Users : Table() {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 50).uniqueIndex()
    val password = varchar("password", 100)
    val email = varchar("email", 100).nullable() // Cho phép NULL
    override val primaryKey = PrimaryKey(id)
}

// DTO
@Serializable
data class UserDTO(
    val username: String,
    val password: String,
    val email: String? = null
)

fun main() {
    // Kết nối PostgreSQL
    Database.connect(
        url = "jdbc:postgresql://localhost:5432/sleepalertdb",
        driver = "org.postgresql.Driver",
        user = "postgres",
        password = "root"
    )

    // Tạo bảng nếu chưa có
    transaction {
        create(Users)
        println("Table 'users' ready.")
    }

    embeddedServer(Netty, host = "0.0.0.0", port = 8080) {
        install(ContentNegotiation) { json() }

        routing {
            // ========================================
            // ĐĂNG KÝ - ĐÃ SỬA: BẮT LỖI + LOG RÕ RÀNG
            // ========================================
            post("/register") {
                println("\n=== Received /register request ===")
                val req = try {
                    call.receive<UserDTO>()
                } catch (e: Exception) {
                    println("Invalid JSON: ${e.message}")
                    call.respond(mapOf("status" to "error", "message" to "Dữ liệu không hợp lệ"))
                    return@post
                }

                println("Username: ${req.username}, Email: ${req.email ?: "null"}")

                // Kiểm tra username tồn tại
                val exists = transaction {
                    Users.select { Users.username eq req.username }.count() > 0
                }

                if (exists) {
                    println("User already exists")
                    call.respond(mapOf("status" to "error", "message" to "Tài khoản đã tồn tại"))
                    return@post
                }

                // Thực hiện insert với try-catch
                try {
                    transaction {
                        Users.insert {
                            it[username] = req.username
                            it[password] = req.password
                            it[email] = req.email  // NULL nếu không có
                        }
                        println("INSERT SQL EXECUTED SUCCESSFULLY")
                    }
                    println("User registered successfully in DB")
                    call.respond(mapOf("status" to "success", "message" to "Đăng ký thành công"))
                } catch (e: Exception) {
                    println("INSERT FAILED: ${e.message}")
                    e.printStackTrace()
                    call.respond(mapOf("status" to "error", "message" to "Lỗi hệ thống: ${e.message}"))
                }
            }

            // ========================================
            // ĐĂNG NHẬP
            // ========================================
            post("/login") {
                val req = try {
                    call.receive<UserDTO>()
                } catch (e: Exception) {
                    call.respond(mapOf("status" to "error", "message" to "Dữ liệu không hợp lệ"))
                    return@post
                }

                val user = transaction {
                    Users.select { Users.username eq req.username }.singleOrNull()
                }

                when {
                    user == null -> {
                        println("Login failed: User not found")
                        call.respond(mapOf("status" to "error", "message" to "Tên đăng nhập không tồn tại"))
                    }
                    user[Users.password] != req.password -> {
                        println("Login failed: Wrong password")
                        call.respond(mapOf("status" to "error", "message" to "Sai mật khẩu"))
                    }
                    else -> {
                        println("Login successful: ${req.username}")
                        call.respond(mapOf("status" to "success", "message" to "Đăng nhập thành công"))
                    }
                }
            }

            // ========================================
            // QUÊN MẬT KHẨU - SỬA: KIỂM TRA EMAIL RỖNG
            // ========================================
            post("/forgot-password") {
                val req = try {
                    call.receive<Map<String, String>>()
                } catch (e: Exception) {
                    call.respond(mapOf("status" to "error", "message" to "Dữ liệu không hợp lệ"))
                    return@post
                }

                val email = req["email"]?.trim() ?: ""

                if (email.isBlank()) {
                    call.respond(mapOf("status" to "error", "message" to "Vui lòng nhập email"))
                    return@post
                }

                val user = transaction {
                    Users.select { Users.email eq email }.singleOrNull()
                }

                if (user == null) {
                    println("Forgot password: Email not found: $email")
                    call.respond(mapOf("status" to "error", "message" to "Email không tồn tại"))
                } else {
                    println("Forgot password: Email found: $email")
                    call.respond(mapOf("status" to "success", "message" to "Liên kết đặt lại mật khẩu đã gửi"))
                }
            }
        }
    }.start(wait = true)
}