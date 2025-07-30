package edu.virginia.cs.Synthesizer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PrintOrder implements Serializable {
    public static List<String> getOutPutOrders(String instFile) {
        List<String> printOrder = new ArrayList<>();
        String fileName = instFile.toLowerCase();

        if (fileName.contains("customerorder")) {
            printOrder.add("Customer");
            printOrder.add("PreferredCustomer");
            printOrder.add("GoldenCustomer");
            printOrder.add("OOrder");
            printOrder.add("CustomerOrderAssociation");
        } else if (fileName.contains("csos")) {
            printOrder.add("Principal");
            printOrder.add("Channel");
            printOrder.add("ProcessStateMachine");
            printOrder.add("ProcessStateMachineState");
            printOrder.add("ProcessStateMachineAction");
            printOrder.add("ProcessStateMachineEvent");
            printOrder.add("ProcessStateMachineTransition");
            printOrder.add("ProcessStateMachineExecution");
            printOrder.add("EmailChannel");
            printOrder.add("SecEmailChannel");
            printOrder.add("SMSChannel");
            printOrder.add("Person");
            printOrder.add("Viewer");
            printOrder.add("Institution");
            printOrder.add("PrincipalProxy");
            printOrder.add("MachineStates");
            printOrder.add("StateMachineEvents");
            printOrder.add("StateMachineTransitions");
        } else if (fileName.contains("ecommerce")) {
            printOrder.add("Customer");
            printOrder.add("OOrder");
            printOrder.add("ShippingCart");
            printOrder.add("Item");
            printOrder.add("Product");
            printOrder.add("Category");
            printOrder.add("CCatalog");
            printOrder.add("Asset");
            printOrder.add("CartItem");
            printOrder.add("OrderItem");
            printOrder.add("PhysicalProduct");
            printOrder.add("ElectronicProduct");
            printOrder.add("Service");
            printOrder.add("Media");
            printOrder.add("Documents");
            printOrder.add("CustomerOrderAssociation");
            printOrder.add("CustomerShippingCartAssociation");
            printOrder.add("ShippingCartItemAssociation");
            printOrder.add("OrderItemAssociation");
            printOrder.add("ProductCategoryAssociation");
            printOrder.add("ProductCatalogAssociation");
            printOrder.add("ProductItemAssociation");
            printOrder.add("ProductAssetAssociation");

        } else if (fileName.contains("decider")) {
            printOrder.add("DecisionSpace");
            printOrder.add("NameSpace");
            printOrder.add("VVariable");
            printOrder.add("Relationship");
            printOrder.add("RRole");
            printOrder.add("Participant");
            printOrder.add("UUser");
            printOrder.add("Viewer");
            printOrder.add("Developer");
            printOrder.add("varAssociation");
            printOrder.add("RoleBindingsAssociation");
            printOrder.add("descisionSpaceParticipantsAssociation");
            printOrder.add("descisionSpaceVariablesAssociation");
            printOrder.add("DSNNamespaceAssociation");

        } else if (fileName.contains("person")) {
            printOrder.add("Person");
            printOrder.add("Student");
            printOrder.add("Employee");
            printOrder.add("Clerk");
            printOrder.add("Manager");

        } else if (fileName.contains("wordpress")) {
            printOrder.add("CommentMeta");
            printOrder.add("Comments");
            printOrder.add("Links");
            printOrder.add("PostMeta");
            printOrder.add("Posts");
            printOrder.add("Pages");
            printOrder.add("UserMeta");
            printOrder.add("Users");
            printOrder.add("Terms");
            printOrder.add("Tags");
            printOrder.add("Category");
            printOrder.add("PostCategory");
            printOrder.add("LinkCategory");
            printOrder.add("CommentPostAssociation");
            printOrder.add("CommentUserAssociation");
            printOrder.add("PostUserAssociation");
            printOrder.add("TermPostsAssociation");
            printOrder.add("TermLinksAssociation");

        } else if (fileName.contains("moodle")) {
            printOrder.add("Course");
            printOrder.add("GradeItem");
            printOrder.add("Grades");
            printOrder.add("ScaleGrades");
            printOrder.add("PointGrades");
            printOrder.add("GradeSettings");
            printOrder.add("ImportNewItem");
            printOrder.add("ImportValues");
            printOrder.add("CourseGradeItemAssociation");
            printOrder.add("CourseGradeSettingsAssociation");

        } else if (fileName.contains("ke")) {
            printOrder.add("Response");

        } else if (fileName.contains("testorder")) {
            printOrder.add("Customer");
            printOrder.add("Test");
            printOrder.add("Order");
            printOrder.add("CustomerOrderAssociation");
            printOrder.add("TestOrderAssociation");

        } else if (fileName.contains("flagship")) {
            printOrder.add("UUser");
            printOrder.add("GGroup");
            printOrder.add("Background");
            printOrder.add("Category");
            printOrder.add("Document");
            printOrder.add("Revision");
            printOrder.add("GroupUserAssociation");
            printOrder.add("CategoryGroupAssociation");
            printOrder.add("CategoryUserAssociation");
            printOrder.add("CategoryBackgroundAssociation");
            printOrder.add("DocumentCategoryAssociation");
            printOrder.add("DocumentUserAssociation");
            printOrder.add("RevisionDocumentAssociation");
            printOrder.add("RevisionUserAssociation");
        }
        return printOrder;
    }
}
