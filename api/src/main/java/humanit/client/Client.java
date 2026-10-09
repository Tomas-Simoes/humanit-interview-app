package humanit.client;

import humanit.document.Document;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "clients",
    uniqueConstraints = {
      @UniqueConstraint(name = "uk_clients_tax_identifier", columnNames = "tax_identifier"),
      @UniqueConstraint(name = "uk_clients_email", columnNames = "email")
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Client {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "first_name", nullable = false, length = 100)
  private String firstName;

  @Column(name = "last_name", nullable = false, length = 100)
  private String lastName;

  @Column(name = "tax_identifier", nullable = false, length = 50)
  private String taxIdentifier;

  @Column(nullable = false, length = 255)
  private String email;

  @Column(name = "phone_number", nullable = false, length = 30)
  private String phoneNumber;

  @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Document> documents = new ArrayList<>();

  public Client(
      String firstName, String lastName, String taxIdentifier, String email, String phoneNumber) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.taxIdentifier = taxIdentifier;
    this.email = email;
    this.phoneNumber = phoneNumber;
  }

  public void updateDetails(
      String firstName, String lastName, String taxIdentifier, String email, String phoneNumber) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.taxIdentifier = taxIdentifier;
    this.email = email;
    this.phoneNumber = phoneNumber;
  }

  public void addDocument(Document document) {
    documents.add(document);
    document.setClient(this);
  }

  public void removeDocument(Document document) {
    documents.remove(document);
    document.setClient(null);
  }
}
