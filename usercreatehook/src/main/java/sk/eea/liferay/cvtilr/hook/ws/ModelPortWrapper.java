package sk.eea.liferay.cvtilr.hook.ws;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import sk.eea.liferay.cvtilr.hook.exception.WSException;
import sk.eea.liferay.cvtilr.hook.service.CustomUserLocalServiceImpl;

import com.evolveum.midpoint.model.client.ModelClientUtil;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaOperationListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.SelectorQualifiedGetOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TimeIntervalStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelPortType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelService;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

/**
 * @author semancik
 * @author Marian Soban
 */
public class ModelPortWrapper {

    private static final Log LOG = LogFactoryUtil.getLog(ModelPortWrapper.class);

    public static final String NS_COMMON = "http://midpoint.evolveum.com/xml/ns/public/common/common-3";
    private static final QName COMMON_PATH = new QName(NS_COMMON, "path");
    private static final QName COMMON_VALUE = new QName(NS_COMMON, "value");
    private static final QName COMMON_ASSIGNMENT = new QName(NS_COMMON, "assignment");

    private static final String FULL_NAME = "fullName";
    private static final String GIVEN_NAME = "givenName";
    private static final String FAMILY_NAME = "familyName";
    private static final String ASSIGNMENT = "assignment";

    private static final String NS_TYPES = "http://prism.evolveum.com/xml/ns/public/types-3";
    private static final QName TYPES_CLEAR_VALUE = new QName(NS_TYPES, "clearValue");
    
    
    private static final QName TYPES_POLYSTRING_ORIG = new QName(NS_TYPES, "orig");

    private static ModelPortType modelPortType;

    /**
     * Private constructor.
     */
    private ModelPortWrapper() {
    }

    /**
     * Should be called to check accessibility of MidPoint from LR.
     * 
     * @return <i>true</i> if successfull ping.
     */
    public static boolean ping() {

        boolean result = false;
        try {
            ModelPortType modelPort = getModelPort();
            // TODO change to call something less demanding
            listRequestableRoles(modelPort);
            result = true;
        } catch (Exception e) {
            LOG.error("Error while ping: " + e.getMessage(), e);
        }
        return result;
    }

    /**
     * Should be called when user is deleted in LR.
     */
    public static boolean deleteUser(String name) {

        Validate.notEmpty(name);

        boolean result = false;
        try {
            ModelPortType modelPort = getModelPort();
            UserType user = searchUserByName(modelPort, StringEscapeUtils.escapeXml(name));
            if (user == null) {
                // user with given screenname not found
                LOG.error("User with given screenname '" + name + "' not found in midpoint - giving up!");
            } else {
                LOG.info("Deleting user with oid '" + user.getOid() + "'");
                deleteUser(modelPort, user.getOid());
                LOG.info("User with oid '" + user.getOid() + "' deleted successfully.");
                result = true;
            }
        } catch (Exception e) {
            LOG.error("Error while user delete in midpoint: " + e.getMessage(), e);
        }
        return result;
    }

    /**
     * Should be called when user's name is changed in LR.
     */
    public static boolean changeName(String name, String newFirstName, String newLastName, String newFullName) {

        Validate.notEmpty(name);
        Validate.notEmpty(newFirstName);
        Validate.notEmpty(newLastName);
        Validate.notEmpty(newFullName);

        boolean result = false;
        try {
            ModelPortType modelPort = getModelPort();
            UserType user = searchUserByName(modelPort, StringEscapeUtils.escapeXml(name));
            if (user == null) {
                // user with given screenname not found
                LOG.error("User with given screenname '" + name + "' not found in midpoint - giving up!");
            } else {
                LOG.info("Setting new name for user with oid '" + user.getOid() + "'");
                changeUserName(modelPort, user.getOid(), newFirstName, newLastName, newFullName);
                LOG.info("New user name set successfully for user with oid '" + user.getOid() + "'");
                result = true;
            }
        } catch (Exception e) {
            LOG.error("Error while changing user name in midpoint: " + e.getMessage(), e);
        }
        return result;
    }

    private static void changeUserName(ModelPortType modelPort,
            String oid,
            String newFirstName,
            String newLastName,
            String newFullName) throws FaultMessage {
        Document doc = getDocumnent();

        ObjectDeltaType userDelta = new ObjectDeltaType();
        userDelta.setOid(oid);
        userDelta.setObjectType(ModelClientUtil.getTypeQName(UserType.class));
        userDelta.setChangeType(ChangeTypeType.MODIFY);

        if (newFirstName != null) {
            ItemDeltaType nameDelta = new ItemDeltaType();
            nameDelta.setModificationType(ModificationTypeType.REPLACE);
            nameDelta.setPath(createItemPathType(GIVEN_NAME));
            nameDelta.getValue().add(createPolyStringType(newFirstName, doc));
            userDelta.getItemDelta().add(nameDelta);
        }

        if (newLastName != null) {
            ItemDeltaType nameDelta = new ItemDeltaType();
            nameDelta.setModificationType(ModificationTypeType.REPLACE);
            nameDelta.setPath(createItemPathType(FAMILY_NAME));
            nameDelta.getValue().add(createPolyStringType(newLastName, doc));
            userDelta.getItemDelta().add(nameDelta);
        }

        if (newFullName != null) {
            ItemDeltaType nameDelta = new ItemDeltaType();
            nameDelta.setModificationType(ModificationTypeType.REPLACE);
            nameDelta.setPath(createItemPathType(FULL_NAME));
            nameDelta.getValue().add(createPolyStringType(newFullName, doc));
            userDelta.getItemDelta().add(nameDelta);
        }
        
        ObjectDeltaListType deltaList = new ObjectDeltaListType();
        deltaList.getDelta().add(userDelta);
        
        modelPort.executeChanges(deltaList, null);
    }

    private static void deleteUser(ModelPortType modelPort, String oid) throws FaultMessage {
    	 ObjectDeltaType deltaType = new ObjectDeltaType();
         deltaType.setObjectType(ModelClientUtil.getTypeQName(UserType.class));
         deltaType.setChangeType(ChangeTypeType.DELETE);
         deltaType.setOid(oid);

         ObjectDeltaListType deltaListType = new ObjectDeltaListType();
         deltaListType.getDelta().add(deltaType);

         modelPort.executeChanges(deltaListType, null);
    }

    /**
     * Should be called when user is verified in LR after creation.
     * 
     * @return <i>true</i> if user is created successfully.
     */
    public static boolean createUser(String email,
            String name,
            String password,
            String firstName,
            String lastName,
            String fullName,
            String organizationName,
            String subOrganizationName,
            List<String> roles) {
        Validate.notEmpty(name);
        Validate.notEmpty(firstName);
        Validate.notEmpty(lastName);
        Validate.notEmpty(fullName);
        Validate.notEmpty(password);
        boolean result = false;
        try {
            ModelPortType modelPort = getModelPort();

            UserType user = searchUserByName(modelPort, StringEscapeUtils.escapeXml(name));
            if (user != null) {
                // user with given screenname already exists
                LOG.warn("User with given screenname '" + name + "' already exists in midpoint with oid '"
                        + user.getOid() + "' - giving up!");
            } else {
                List<RoleType> roleTypes = getRoleTypesForLiferayRoles(modelPort, roles);
                // create user
                String userOid = createUserWithRoles(modelPort, roleTypes, email, name, password, firstName, lastName,
                        fullName, organizationName, subOrganizationName);
                LOG.info("User with oid '" + userOid + "' created for screenname '" + name + "' successfully");
                result = true;
            }
        } catch (Exception e) {
            LOG.error("Error while user creation in midpoint: " + e.getMessage(), e);
        }
        return result;
    }

    private static List<RoleType> getRoleTypesForLiferayRoles(ModelPortType modelPort, List<String> roles)
            throws FaultMessage, SAXException, IOException, JAXBException {
        List<RoleType> roleTypes = new ArrayList<RoleType>();
        if (roles != null) {
            for (String roleName : roles) {
                if (CustomUserLocalServiceImpl.DEFAULT_LIFERAY_ROLE.equals(roleName)) {
                    continue; // XXX ignore default Liferay's role 'User'
                }
                RoleType roleType = searchRoleByName(modelPort, StringEscapeUtils.escapeXml(roleName));
                if (roleType != null) {
                    roleTypes.add(roleType);
                } else {
                    LOG.warn("Role '" + roleName + "' not found in midpoint - ignoring role");
                }
            }
        }
        return roleTypes;
    }

    /**
     * Should be called whenever password is changed in LR.
     * 
     * @param name screenname of user
     * @return <i>true</i> if password changed successfully.
     * @throws Exception 
     */
    public static boolean changePassword(String name, String password) throws Exception {
        Validate.notEmpty(name);
        Validate.notEmpty(password);

        boolean result = false;
        try {
            ModelPortType modelPort = getModelPort();
            UserType user = searchUserByName(modelPort, StringEscapeUtils.escapeXml(name));
            if (user == null) {
                // user with given screenname not found
                LOG.error("User with given screenname '" + name + "' not found in midpoint - giving up!");
                throw new WSException("No such user"); 
            } else {
                LOG.info("Setting password for user with oid '" + user.getOid() + "'");
                changeUserPassword(modelPort, user.getOid(), password);
                LOG.info("Password set successfully for user with oid '" + user.getOid() + "'");
                result = true;
            }
        } catch (Exception e) {
            LOG.error("Error while password change in midpoint: " + e.getMessage(), e);
            throw e;
        }
        return result;
    }

    public static Boolean existsUser(String name) {
        Validate.notEmpty(name);

        Boolean result = null;
        try {
            ModelPortType modelPort = getModelPort();
            UserType user = searchUserByName(modelPort, StringEscapeUtils.escapeXml(name));
            if (user != null) {
                result = Boolean.TRUE;
            } else {
                result = Boolean.FALSE;
            }

        } catch (Exception e) {
            LOG.error("Error user exist check in midpoint: " + e.getMessage(), e);
        }
        return result;
    }

    /**
     * Should be called whenever user roles are changed in LR.
     * 
     * @param name screenname of user
     * @return <i>true</i> if roles were changed successfully.
     */
    public static boolean changeRoles(String name, List<String> rolesToAdd, List<String> rolesToRemove) {
        Validate.notEmpty(name);

        boolean result = false;
        try {
            ModelPortType modelPort = getModelPort();
            UserType user = searchUserByName(modelPort, StringEscapeUtils.escapeXml(name));
            if (user == null) {
                // user with given screenname not found
                LOG.error("User with given screenname '" + name + "' not found in midpoint - giving up!");
            } else {
                LOG.info("Setting roles for user with oid '" + user.getOid() + "'");
                List<RoleType> roleTypesToAdd = getRoleTypesForLiferayRoles(modelPort, rolesToAdd);
                List<RoleType> roleTypesToRemove = getRoleTypesForLiferayRoles(modelPort, rolesToRemove);

                if (!roleTypesToAdd.isEmpty()) {
                    List<String> roleTypeOids = getRoleTypeOids(roleTypesToAdd);
                    LOG.info("  -adding roles '" + roleTypeOids + "' for user with oid '" + user.getOid() + "'");
                    assignRoles(modelPort, user.getOid(), roleTypeOids);
                }
                if (!roleTypesToRemove.isEmpty()) {
                    List<String> roleTypeOids = getRoleTypeOids(roleTypesToRemove);
                    LOG.info("  -removing roles '" + roleTypeOids + "' for user with oid '" + user.getOid() + "'");
                    unAssignRoles(modelPort, user.getOid(), getRoleTypeOids(roleTypesToRemove));
                }

                LOG.info("Roles set successfully for user with oid '" + user.getOid() + "'");
                result = true;
            }
        } catch (Exception e) {
            LOG.error("Error while roles change in midpoint: " + e.getMessage(), e);
        }
        return result;
    }

    private static List<String> getRoleTypeOids(List<RoleType> roleTypes) {
        List<String> result = new ArrayList<String>();
        for (RoleType roleType : roleTypes) {
            result.add(roleType.getOid());
        }
        return result;
    }

    // private static SystemConfigurationType getConfiguration(ModelPortType modelPort) throws FaultMessage {
    //
    // Holder<ObjectType> objectHolder = new Holder<ObjectType>();
    // Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
    // OperationOptionsType options = new OperationOptionsType();
    //
    // modelPort.getObject(getTypeUri(SystemConfigurationType.class), SystemObjectsType.SYSTEM_CONFIGURATION.value(),
    // options, objectHolder, resultHolder);
    //
    // return (SystemConfigurationType) objectHolder.value;
    // }

    private static String createUserWithRoles(ModelPortType modelPort,
            List<RoleType> roleTypes,
            String email,
            String name,
            String password,
            String firstName,
            String lastName,
            String fullName,
            String organizationName,
            String subOrganizationName) throws FaultMessage {
        Document doc = getDocumnent();

        UserType user = new UserType();
        user.setName(createPolyStringType(name, doc));
        user.setFullName(createPolyStringType(fullName, doc));
        user.setGivenName(createPolyStringType(firstName, doc));
        user.setFamilyName(createPolyStringType(lastName, doc));
        user.setEmailAddress(email);
        ActivationType activation = new ActivationType();
        activation.setAdministrativeStatus(ActivationStatusType.ENABLED);
        user.setActivation(activation);
        
        if (!StringUtils.isBlank(organizationName)) {
            user.getOrganization().add(createPolyStringType(organizationName, doc));
        }
        if (!StringUtils.isBlank(subOrganizationName)) {
            user.getOrganizationalUnit().add(createPolyStringType(subOrganizationName, doc));
        }
        if (!StringUtils.isBlank(password)) {
            user.setCredentials(createPasswordCredentials(password));
        }

        // handle roles
        assignRoleTypesToUser(user, roleTypes);

        ObjectDeltaType deltaType = new ObjectDeltaType();
        deltaType.setObjectType(ModelClientUtil.getTypeQName(UserType.class));
        deltaType.setChangeType(ChangeTypeType.ADD);
        deltaType.setObjectToAdd(user);

        ObjectDeltaListType deltaListType = new ObjectDeltaListType();
        deltaListType.getDelta().add(deltaType);
		ObjectDeltaOperationListType operationListType = modelPort.executeChanges(deltaListType, null);
        
     
        return getOidFromDeltaOperationList(operationListType, deltaType);
    }
    
    public static String getOidFromDeltaOperationList(ObjectDeltaOperationListType operationListType, ObjectDeltaType originalDelta) {
        ObjectDeltaOperationType odo = findInDeltaOperationList(operationListType, originalDelta);
        return odo != null ? ((ObjectType) odo.getObjectDelta().getObjectToAdd()).getOid() : null;
    }
    
    public static ObjectDeltaOperationType findInDeltaOperationList(ObjectDeltaOperationListType operationListType, ObjectDeltaType originalDelta) {
        Validate.notNull(operationListType);
        Validate.notNull(originalDelta);
        if (originalDelta.getChangeType() != ChangeTypeType.ADD) {
            throw new IllegalArgumentException("Original delta is not of ADD type");
        }
        if (originalDelta.getObjectToAdd() == null) {
            throw new IllegalArgumentException("Original delta contains no object-to-be-added");
        }
        for (ObjectDeltaOperationType operationType : operationListType.getDeltaOperation()) {
            ObjectDeltaType objectDeltaType = operationType.getObjectDelta();
            if (objectDeltaType.getChangeType() == ChangeTypeType.ADD &&
                    objectDeltaType.getObjectToAdd() != null) {
                ObjectType objectAdded = (ObjectType) objectDeltaType.getObjectToAdd();
                if (objectAdded.getClass().equals(originalDelta.getObjectToAdd().getClass())) {
                    return operationType;
                }
            }
        }
        return null;
    }

    private static void assignRoleTypesToUser(UserType user, List<RoleType> roleTypes) {
        Validate.notNull(user);
        user.getAssignment().clear(); // clear roles assignment
        if (roleTypes != null) {
            for (RoleType role : roleTypes) {
                // create user with a role assignment
                AssignmentType roleAssignment = new AssignmentType();
                ObjectReferenceType roleRef = new ObjectReferenceType();
                roleRef.setOid(role.getOid());
                roleRef.setType(getTypeQName(RoleType.class));
                roleAssignment.setTargetRef(roleRef);
                user.getAssignment().add(roleAssignment);
            }
        }
    }

    private static void changeUserPassword(ModelPortType modelPort, String oid, String newPassword) throws FaultMessage {
        Document doc = getDocumnent();

        ItemDeltaType passwordDelta = new ItemDeltaType();
		passwordDelta.setModificationType(ModificationTypeType.REPLACE);
		passwordDelta.setPath(createItemPathType("credentials/password/value"));
        passwordDelta.getValue().add(createProtectedString(newPassword));

        ObjectDeltaType userDelta = new ObjectDeltaType();
        userDelta.setObjectType(ModelClientUtil.getTypeQName(UserType.class));
        userDelta.setChangeType(ChangeTypeType.MODIFY);
        userDelta.setOid(oid);
        userDelta.getItemDelta().add(passwordDelta);

        ObjectDeltaListType deltaListType = new ObjectDeltaListType();
        deltaListType.getDelta().add(userDelta);
        modelPort.executeChanges(deltaListType, null);
    }

    private static void assignRoles(ModelPortType modelPort, String userOid, List<String> roleOids) throws FaultMessage {
        modifyRoleAssignment(modelPort, userOid, true, roleOids);
    }

    private static void unAssignRoles(ModelPortType modelPort, String userOid, List<String> roleOids)
            throws FaultMessage {
        modifyRoleAssignment(modelPort, userOid, false, roleOids);
    }

    private static void modifyRoleAssignment(ModelPortType modelPort,
            String userOid,
            boolean isAdd,
            List<String> roleOids) throws FaultMessage {

    	 ItemDeltaType assignmentDelta = new ItemDeltaType();
         if (isAdd) {
             assignmentDelta.setModificationType(ModificationTypeType.ADD);
         } else {
             assignmentDelta.setModificationType(ModificationTypeType.DELETE);
         }
         
         assignmentDelta.setPath(ModelClientUtil.createItemPathType(ASSIGNMENT));
         
         for (String roleOid : roleOids) {
         	assignmentDelta.getValue().add(createRoleAssignment(roleOid));
         }
         
         ObjectDeltaType userDelta = new ObjectDeltaType();
         userDelta.setObjectType(ModelClientUtil.getTypeQName(UserType.class));
         userDelta.setChangeType(ChangeTypeType.MODIFY);
         userDelta.setOid(userOid);
         userDelta.getItemDelta().add(assignmentDelta);

         ObjectDeltaListType deltaListType = new ObjectDeltaListType();
         deltaListType.getDelta().add(userDelta);

         modelPort.executeChanges(deltaListType, null);
    }

    private static AssignmentType createRoleAssignment(String roleOid) {
        AssignmentType roleAssignment = new AssignmentType();
        ObjectReferenceType roleRef = new ObjectReferenceType();
        roleRef.setOid(roleOid);
        roleRef.setType(getTypeQName(RoleType.class));
        roleAssignment.setTargetRef(roleRef);
        return roleAssignment;
    }

    private static UserType searchUserByName(ModelPortType modelPort, String username) throws SAXException,
            IOException, FaultMessage, JAXBException {
        // WARNING: in a real case make sure that the username is properly escaped before putting it in XML
    	SearchFilterType filter = ModelClientUtil.parseSearchFilterType(
				"<equal xmlns='http://prism.evolveum.com/xml/ns/public/query-3' xmlns:c='http://midpoint.evolveum.com/xml/ns/public/common/common-3' >" +
				  "<path>c:name</path>" +
				  "<value>" + username + "</value>" +
				"</equal>"
		);
		QueryType query = new QueryType();
		query.setFilter(filter);
		SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();
        Holder<ObjectListType> objectListHolder = new Holder<ObjectListType>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();

        modelPort.searchObjects(getTypeQName(UserType.class), query, options, objectListHolder, resultHolder);
        
        ObjectListType objectList = objectListHolder.value;
        List<ObjectType> objects = objectList.getObject();
        if (objects.isEmpty()) {
            return null;
        }
        if (objects.size() == 1) {
            return (UserType) objects.get(0);
        }
        throw new IllegalStateException("Expected to find a single user with username '" + username + "' but found "
                + objects.size() + " roles instead");
    }

    private static RoleType searchRoleByName(ModelPortType modelPort, String roleName) throws SAXException,
            IOException, FaultMessage, JAXBException {
        // WARNING: in a real case make sure that the username is properly escaped before putting it in XML
    	SearchFilterType filter = ModelClientUtil.parseSearchFilterType(
				"<equal xmlns='http://prism.evolveum.com/xml/ns/public/query-3' xmlns:c='http://midpoint.evolveum.com/xml/ns/public/common/common-3' >" +
				  "<path>c:name</path>" +
				  "<value>" + roleName + "</value>" +
				"</equal>"
		);
		QueryType query = new QueryType();
		query.setFilter(filter);
        SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();
        Holder<ObjectListType> objectListHolder = new Holder<ObjectListType>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();

        modelPort.searchObjects(getTypeQName(RoleType.class), query, options, objectListHolder, resultHolder);

        ObjectListType objectList = objectListHolder.value;
        List<ObjectType> objects = objectList.getObject();
        if (objects.isEmpty()) {
            return null;
        }
        if (objects.size() == 1) {
            return (RoleType) objects.get(0);
        }
        throw new IllegalStateException("Expected to find a single role with name '" + roleName + "' but found "
                + objects.size() + " users instead");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Collection<RoleType> listRequestableRoles(ModelPortType modelPort) throws SAXException, IOException,
            FaultMessage, JAXBException {
    	SearchFilterType filter = ModelClientUtil.parseSearchFilterType(
				"<equal xmlns='http://prism.evolveum.com/xml/ns/public/query-3' xmlns:c='http://midpoint.evolveum.com/xml/ns/public/common/common-3' >" +
				  "<path>c:requestable</path>" +
				  "<value>true</value>" +
				"</equal>"
		);
		QueryType query = new QueryType();
		query.setFilter(filter);
        SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();
        Holder<ObjectListType> objectListHolder = new Holder<ObjectListType>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();

        modelPort.searchObjects(getTypeQName(RoleType.class), query, options, objectListHolder, resultHolder);

        ObjectListType objectList = objectListHolder.value;
        return (Collection) objectList.getObject();
    }

    private static Element parseElement(String stringXml) throws SAXException, IOException {
        Document document = builderLocal.get().parse(IOUtils.toInputStream(stringXml, "utf-8"));
        return getFirstChildElement(document);
    }

    public static Element getFirstChildElement(Node parent) {
        if (parent == null || parent.getChildNodes() == null) {
            return null;
        }

        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node child = nodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                return (Element) child;
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> JAXBElement<T> toJaxbElement(QName name, T value) {
        return new JAXBElement<T>(name, (Class<T>) value.getClass(), value);
    }

    private static Element createPathElement(String stringPath, Document doc) {
        String pathDeclaration = "declare default namespace '" + NS_COMMON + "'; " + stringPath;
        return createTextElement(COMMON_PATH, pathDeclaration, doc);
    }

    private static PolyStringType createPolyStringType(String string, Document doc) {
        PolyStringType polyStringType = new PolyStringType();
        Element origElement = createTextElement(TYPES_POLYSTRING_ORIG, string, doc);
        polyStringType.getContent().add(origElement);
        return polyStringType;
    }

    private static Element createTextElement(QName qname, String value, Document doc) {
        Element element = doc.createElementNS(qname.getNamespaceURI(), qname.getLocalPart());
        element.setTextContent(value);
        return element;
    }

    private static Document getDocumnent() {
        return builderLocal.get().newDocument();
    }

    private static String getTypeUri(Class<? extends ObjectType> type) {
        // QName typeQName = JAXBUtil.getTypeQName(type);
        // String typeUri = QNameUtil.qNameToUri(typeQName);
        String typeUri = NS_COMMON + "#" + type.getSimpleName();
        return typeUri;
    }

    private static QName getTypeQName(Class<? extends ObjectType> type) {
        // QName typeQName = JAXBUtil.getTypeQName(type);
        QName typeQName = new QName(NS_COMMON, type.getSimpleName());
        return typeQName;
    }

    private static CredentialsType createPasswordCredentials(String password) {
        CredentialsType credentialsType = new CredentialsType();
        credentialsType.setPassword(createPasswordType(password));
        return credentialsType;
    }

    private static PasswordType createPasswordType(String password) {
        PasswordType passwordType = new PasswordType();
        passwordType.setValue(createProtectedString(password));
        return passwordType;
    }

    private static ProtectedStringType createProtectedString(String clearValue) {
    	ProtectedStringType protectedString = new ProtectedStringType();
        // this is a bit of workaround: it should be possible to add clearValue by itself, but there seems to be a parsing bug on the server side that needs to be fixed first (TODO)
		protectedString.getContent().add(toJaxbElement(TYPES_CLEAR_VALUE, clearValue));
		return protectedString;
    }

    private static synchronized ModelPortType getModelPort() {
        if (modelPortType == null) {
            // initialize
            modelPortType = createModelPort();
        }
        return modelPortType;
    }

    private static ModelPortType createModelPort() {

        String endpointUrl = WSConfig.getEndPointUrl();
        String username = WSConfig.getUser();

        LOG.info("Endpoint URL: " + endpointUrl);

        ModelService modelService = new ModelService();
        ModelPortType modelPort = modelService.getModelPort();
        BindingProvider bp = (BindingProvider) modelPort;

        // make request context thread safe
        ((BindingProvider) modelPort).getRequestContext().put("thread.local.request.context", "true");

        Map<String, Object> requestContext = bp.getRequestContext();
        requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);

        org.apache.cxf.endpoint.Client client = ClientProxy.getClient(modelPort);
        org.apache.cxf.endpoint.Endpoint cxfEndpoint = client.getEndpoint();

        Map<String, Object> outProps = new HashMap<String, Object>();

        outProps.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
        outProps.put(WSHandlerConstants.USER, username);
        outProps.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_DIGEST);
        outProps.put(WSHandlerConstants.PW_CALLBACK_CLASS, ClientPasswordHandler.class.getName());

        WSS4JOutInterceptor wssOut = new WSS4JOutInterceptor(outProps);
        cxfEndpoint.getOutInterceptors().add(wssOut);

        return modelPort;
    }
    
    public static ItemPathType createItemPathType(String stringPath) {
        ItemPathType itemPathType = new ItemPathType();
        String pathDeclaration = "declare default namespace '" + NS_COMMON + "'; " + stringPath;
        itemPathType.setValue(pathDeclaration);
        return itemPathType;
    }
    private static final ThreadLocal<DocumentBuilder> builderLocal = new ThreadLocal<DocumentBuilder>() {
        @Override
        protected DocumentBuilder initialValue() {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                return factory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw new IllegalStateException("Error creating XML document " + e.getMessage());
            }
        }
    };
}
