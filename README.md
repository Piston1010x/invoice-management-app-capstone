# Invoice Management App

A B2B Invoice Management MVP built with Spring Boot, MySQL, and Thymeleaf.  
Developed as part of the EPAM Java Web Developer capstone project.



## Features

- Secure multi-user login (Spring Security)
- Client and invoice management
- Full invoice lifecycle: Draft → Sent → Paid/Overdue
- PDF invoice generation using OpenPDF
- Email delivery with attached PDF and secure "Mark as Paid" link
- Dashboard with revenue and outstanding charts per currency
- Public invoice confirmation page (no login required)
- Works with Mailtrap for email testing
- Clean, responsive UI using Bootstrap



## Tech Stack

- Java 11  
- Spring Boot 3  
- Spring Data JPA (MySQL)  
- Spring Security  
- Thymeleaf + Bootstrap 5  
- JavaMailSender  
- OpenPDF  



## Getting Started

### Prerequisites

- Java 11 installed  
- Maven installed  
- MySQL running locally  


### 1. Clone the Repository
```bash
git clone https://github.com/YOUR-USERNAME/invoice-management-app.git
cd invoice-management-app
```
### 2. Create the MySQL Database
```bash
CREATE DATABASE invoice_app;
```
### 3. Configure Database Connection
Open src/main/resources/application.properties and update:
```bash
spring.datasource.url=jdbc:mysql://localhost:3306/invoice_app
spring.datasource.username=your_mysql_user
spring.datasource.password=your_mysql_password
```
### 4. Configure Email
For development/testing, use Mailtrap or any other SMTP service
Open src/main/resources/application.properties and update:
```bash
spring.mail.host=smtp.mailtrap.io
spring.mail.port=2525
spring.mail.username=your_mailtrap_username
spring.mail.password=your_mailtrap_password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```
### 5. Run the app
if Maven is installed globally:
```bash
mvn spring-boot:run
```
### 6. Access the App
Open your browser and go to:
```bash
http://localhost:8080
```







    
    
