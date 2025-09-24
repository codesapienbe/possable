# POSsable: Simple Restaurant POS SaaS API

This project is a backend REST API for a simple restaurant Point of Sale (POS) system designed as a SaaS application. It is built with Spring Boot 3, leveraging Virtual Threads for high concurrency and GraalVM for native image compilation to ensure high performance and quick startup.

## Features

- **Order Management:** Create, update, and track restaurant orders.
- **Menu Items:** Manage menu items including pricing and availability.
- **Employee Management:** Add and manage restaurant staff.
- **Payments:** Record payments with multiple payment methods.
- **Authentication:** Secure API access with API secret keys and bearer tokens.
- **Usage Limits:** Enforce monthly API usage limits for SaaS clients.
- **Multi-Printer Support:** Register multiple printers by category (oven, grill, cashier, customer copy).
- **Print Templates:** Define and manage flexible print templates per printer category.
- **Print Queue:** Assign print jobs to specific printers with templates and track the status of print jobs (pending, printing, completed, failed).
- **Customer Copy Printing:** Generate customer receipt copies with order and tracking numbers for a better waiting experience.

## Technology Stack

- **Backend:** Spring Boot 3
- **Concurrency:** Virtual Threads (Java 21+)
- **Native Compilation:** GraalVM Native Image
- **Authentication:** API Key + JWT Bearer Token
- **API Definition:** OpenAPI 3.1 YAML specification
- **Build Tool:** Maven or Gradle (configurable)
- **Database:** (Implementation dependent, recommended PostgreSQL or any JPA-compatible DB)

## Getting Started

### Prerequisites

- Java 21 or later
- GraalVM with native-image support installed
- Maven or Gradle build system
- PostgreSQL or other supported database

### Installation

1. Clone the repository:

   ```bash
   git clone https://github.com/yourusername/simple-restaurant-pos.git
   ```

2. Build the native image (optional for production):

   ```bash
   ./mvnw spring-boot:build-image
   ```

3. Configure application properties (`application.yml` or `application.properties`) with your database credentials and auth secrets.

4. Run the application:

   ```bash
   ./mvnw spring-boot:run
   ```
   
   Or run the native image binary directly.

### API Documentation

The API is fully described in the `openapi.yaml` file located in this repository root. It details all endpoints, request/response models, and security schemes.

You can use tools like Swagger UI or Redoc to visualize and interact with the API endpoints.

## Authentication

- Clients must provide an API secret key in the `X-API-KEY` header to identify the client account.
- End-user authentication is performed via Bearer JWT tokens.
- Monthly usage limits are enforced per client to manage SaaS quotas.

## Printing Workflow

- Register printers with categories such as oven, grill, cashier, or customer copy.
- Create and manage print templates assigned to each printer category.
- Create print jobs linked to orders that send the formatted print content to the specific printer.
- Track print job lifecycle through statuses to ensure reliable printing.
- Customer copy prints include order number and tracking information to improve customer experience.

## Contributing

Contributions are welcome! Please fork the repository and create a pull request with improvements or bug fixes.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contact

For questions, issues, or feature requests, please open an issue in this repository or contact [your-contact-email@domain.com].

---

