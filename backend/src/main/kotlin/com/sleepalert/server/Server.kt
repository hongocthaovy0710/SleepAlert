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

// =======================
// BẢNG USERS (Exposed)
// =======================
object Users : Table() {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 50).uniqueIndex()
    val password = varchar("password", 100)
    val email = varchar("email", 100).nullable() // Cho phép NULL
    override val primaryKey = PrimaryKey(id)
}

// =======================
// DTO REQUEST / RESPONSE
// =======================
@Serializable
data class UserDTO(
    val username: String,
    val password: String,
    val email: String? = null
)

@Serializable
data class ApiResponse(
    val status: String,
    val message: String
)

fun main() {
    // Kết nối PostgreSQL
    Database.connect(
        url = "jdbc:postgresql://localhost:5432/sleepalertdb",
        driver = "org.postgresql.Driver",
        user = "postgres",
        password = "root"   // nhớ đúng với mật khẩu em đặt trong pgAdmin
    )

    // Tạo bảng nếu chưa có
    transaction {
        create(Users)
        println("Table 'users' ready.")
    }

    embeddedServer(Netty, host = "0.0.0.0", port = 8080) {
        // Bật JSON
        install(ContentNegotiation) {
            json()
        }

        routing {

            // ========================================
            // ĐĂNG KÝ
            // ========================================
            post("/register") {
                println("\n=== Received /register request ===")

                val req = try {
                    call.receive<UserDTO>()
                } catch (e: Exception) {
                    println("Invalid JSON in /register: ${e.message}")
                    call.respond(ApiResponse("error", "Dữ liệu không hợp lệ"))
                    return@post
                }

                println("Username: ${req.username}, Email: ${req.email ?: "null"}")

                // Kiểm tra username tồn tại
                val exists = try {
                    transaction {
                        Users.select { Users.username eq req.username }.count() > 0
                    }
                } catch (e: Exception) {
                    println("DB error when checking exists: ${e.message}")
                    e.printStackTrace()
                    call.respond(ApiResponse("error", "Lỗi database khi kiểm tra tài khoản"))
                    return@post
                }

                if (exists) {
                    println("User already exists")
                    call.respond(ApiResponse("error", "Tài khoản đã tồn tại"))
                    return@post
                }

                // Thực hiện insert
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
                    call.respond(ApiResponse("success", "Đăng ký thành công"))
                } catch (e: Exception) {
                    println("INSERT FAILED: ${e.message}")
                    e.printStackTrace()
                    call.respond(ApiResponse("error", "Lỗi hệ thống: ${e.message}"))
                }
            }

            // ========================================
            // ĐĂNG NHẬP
            // ========================================
            post("/login") {
                val req = try {
                    call.receive<UserDTO>()
                } catch (e: Exception) {
                    println("Invalid JSON in /login: ${e.message}")
                    call.respond(ApiResponse("error", "Dữ liệu không hợp lệ"))
                    return@post
                }

                val user = try {
                    transaction {
                        Users.select { Users.username eq req.username }.singleOrNull()
                    }
                } catch (e: Exception) {
                    println("DB error in /login: ${e.message}")
                    e.printStackTrace()
                    call.respond(ApiResponse("error", "Lỗi database"))
                    return@post
                }

                when {
                    user == null -> {
                        println("Login failed: User not found")
                        call.respond(ApiResponse("error", "Tên đăng nhập không tồn tại"))
                    }
                    user[Users.password] != req.password -> {
                        println("Login failed: Wrong password")
                        call.respond(ApiResponse("error", "Sai mật khẩu"))
                    }
                    else -> {
                        println("Login successful: ${req.username}")
                        call.respond(ApiResponse("success", "Đăng nhập thành công"))
                    }
                }
            }

            // ========================================
            // QUÊN MẬT KHẨU
            // ========================================
            post("/forgot-password") {
                val req = try {
                    call.receive<Map<String, String>>()
                } catch (e: Exception) {
                    println("Invalid JSON in /forgot-password: ${e.message}")
                    call.respond(ApiResponse("error", "Dữ liệu không hợp lệ"))
                    return@post
                }

                val email = req["email"]?.trim() ?: ""

                if (email.isBlank()) {
                    call.respond(ApiResponse("error", "Vui lòng nhập email"))
                    return@post
                }

                val user = try {
                    transaction {
                        Users.select { Users.email eq email }.singleOrNull()
                    }
                } catch (e: Exception) {
                    println("DB error in /forgot-password: ${e.message}")
                    e.printStackTrace()
                    call.respond(ApiResponse("error", "Lỗi database"))
                    return@post
                }

                if (user == null) {
                    println("Forgot password: Email not found: $email")
                    call.respond(ApiResponse("error", "Email không tồn tại"))
                } else {
                    println("Forgot password: Email found: $email")
                    call.respond(ApiResponse("success", "Liên kết đặt lại mật khẩu đã gửi"))
                }
            }
        }
    }.start(wait = true)
}
