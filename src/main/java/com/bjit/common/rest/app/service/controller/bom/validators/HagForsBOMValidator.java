/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bjit.common.rest.app.service.controller.bom.validators;

import com.bjit.common.rest.app.service.controller.bom.interfaces.IBomValidator;
import com.bjit.common.rest.app.service.controller.bom.processor.CommonBOMImportParams;
import com.bjit.common.rest.app.service.model.createBOM.CreateBOMBean;
import com.bjit.common.rest.app.service.model.tnr.TNR;
import com.bjit.common.rest.app.service.utilities.BusinessObjectOperations;
import com.bjit.common.rest.app.service.utilities.CommonSearch;
import com.bjit.common.rest.app.service.utilities.CommonUtilities;
import com.bjit.common.rest.app.service.utilities.DateTimeUtils;
import com.bjit.common.rest.app.service.utilities.NullOrEmptyChecker;
import com.bjit.common.rest.item_bom_import.utility.BusinessObjectUtil;
import com.bjit.common.rest.item_bom_import.utility.bomResponseMessageFormatter.BOMDataCollector;
import com.bjit.common.rest.item_bom_import.utility.bomResponseMessageFormatter.ChildInfo;
import com.bjit.common.rest.item_bom_import.utility.bomResponseMessageFormatter.ParentInfo;
import com.bjit.common.rest.item_bom_import.xml_BOM_mapper_model.Attribute;
import com.bjit.common.rest.item_bom_import.xml_BOM_mapper_model.Relationship;
import com.bjit.ewc18x.utils.PropertyReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.util.MatrixException;

/**
 *
 * @author BJIT
 */
public final class HagForsBOMValidator implements IBomValidator {

    private static final org.apache.log4j.Logger HAGFORS_BOM_IMPORT_VALIDATOR_LOGGER = org.apache.log4j.Logger.getLogger(HagForsBOMValidator.class);
    private static final String POSITIVE_INTEGER_REGEX = "[1-9]\\d*";
    private static final Pattern POSITIVE_INTEGER_PATTERN = Pattern.compile(POSITIVE_INTEGER_REGEX);

    public HagForsBOMValidator() {
    }

    /**
     *
     * @param businessObjectUtil
     * @param businessObjectOperations
     * @param createBOMBean
     * @param context
     * @param commonBomImportVariables
     * @return
     * @throws IOException
     * @throws MatrixException
     */
    @Override
    public BOMDataCollector bomValidationAndDataCollection(Context context, BusinessObjectUtil businessObjectUtil, BusinessObjectOperations businessObjectOperations, CreateBOMBean createBOMBean, CommonBOMImportParams commonBomImportVariables) throws IOException, MatrixException, Exception {
        List<Relationship> relationshipList = commonBomImportVariables.relationshipList;
        String relName = commonBomImportVariables.relName;
        String interfaceName = commonBomImportVariables.interfaceName;

        BOMDataCollector dataCollector = new BOMDataCollector();
        HashMap<ParentInfo, HashMap<String, ChildInfo>> requestParentChildInfoMap = new HashMap<>();
        HashMap<String, ChildInfo> childInfoMap = new HashMap<>();
        HashMap<String, HashMap> existingChildDataCollectorMap = new HashMap<>();
        HashMap<String, ArrayList<String>> existingChildInfoRelMap = new HashMap<>();
        HashMap<String, String> childInfoIdMap = new HashMap<>();
        ParentInfo parentInfo = new ParentInfo();

        TNR parentItem = createBOMBean.getItem();

        String parentType = parentItem.getType();
        String parentName = parentItem.getName();

        HAGFORS_BOM_IMPORT_VALIDATOR_LOGGER.info("Parent BOM validation process has been started");

        TNR parentTNR = parentItemTNRValidation(parentItem);
        parentInfo.setTnr(parentTNR);

        String parentObjectId = "";

        CommonSearch commonSearch = new CommonSearch();
        BusinessObject parentBusinessObject = null;
        try {
            parentBusinessObject = searchParentItem(context, parentTNR, commonSearch);
            parentObjectId = parentBusinessObject.getObjectId();

            List<String> relList = Optional.ofNullable(relationshipList).orElseThrow(() -> new RuntimeException(parentTNR.getType() + " has not been allowed as a parent type")).stream().map((Relationship relationship) -> relationship.getName()).collect(Collectors.toList());

            Instant start_expand_time = Instant.now();
            HAGFORS_BOM_IMPORT_VALIDATOR_LOGGER.debug("Start expanding 1 level structure for parent : " + parentName);

            existingChildDataCollectorMap = businessObjectUtil.getExpandedDataFromParent(context, parentBusinessObject, null, relList, null, null, (short) 1);

            Instant end_expand_time = Instant.now();
            HAGFORS_BOM_IMPORT_VALIDATOR_LOGGER.info(" | Expand | " + parentName + " | " + DateTimeUtils.getDuration(start_expand_time, end_expand_time));

            existingChildInfoRelMap = existingChildDataCollectorMap.get("child-rel-info");
            childInfoIdMap = existingChildDataCollectorMap.get("child-id-info");

        } catch (MatrixException ex) {
            HAGFORS_BOM_IMPORT_VALIDATOR_LOGGER.error(ex);
            throw ex;
        } catch (ArrayIndexOutOfBoundsException | NullPointerException ex) {
            HAGFORS_BOM_IMPORT_VALIDATOR_LOGGER.error(ex);
            throw ex;
        } catch (Exception ex) {
            HAGFORS_BOM_IMPORT_VALIDATOR_LOGGER.error(ex);
            throw ex;
        }

        HAGFORS_BOM_IMPORT_VALIDATOR_LOGGER.info("Parent BOM validation process has been completed");
        HAGFORS_BOM_IMPORT_VALIDATOR_LOGGER.info("Lines validation process has been started");

        HashMap<String, String> duplicatePositionValidatorMap = new HashMap<>();

        for (HashMap<String, String> lineDataMap : createBOMBean.getLines()) {
            ChildInfo childInfo = new ChildInfo();
            childInfo.setLength(lineDataMap.get("length"));
            childInfo.setWidth(lineDataMap.get("width"));

            Optional.ofNullable(lineDataMap.get("Number of units")).ifPresent(numberOfUnits -> {
                childInfo.setChildNoOfUnit(Integer.parseInt(numberOfUnits));
            });

            TNR tempChildTNR = new TNR(lineDataMap.get("type"), lineDataMap.get("component"), lineDataMap.get("revision"));

            String childType = validateChildAttirbutesInTheRequest("type", lineDataMap, tempChildTNR, parentTNR);
            String childName = validateChildAttirbutesInTheRequest("component", lineDataMap, tempChildTNR, parentTNR);
            String childRev = validateChildAttirbutesInTheRequest("revision", lineDataMap, tempChildTNR, parentTNR);
            String childPosition = validateChildAttirbutesInTheRequest("Position", lineDataMap, tempChildTNR, parentTNR);
            String childQuantity = validateChildAttirbutesInTheRequest("Net quantity", lineDataMap, tempChildTNR, parentTNR);
            float childQuantityFloat = Float.parseFloat(childQuantity);

            HAGFORS_BOM_IMPORT_VALIDATOR_LOGGER.debug(tempChildTNR.toString() + " Position " + childPosition);
            tempChildTNR = null;

            TNR childTNR = new TNR(childType, childName, childRev);

            childInfo.setChildTNR(childTNR);
            childInfo.setMessage("");

            String childId = "";
            HashMap<String, String> propertyNameValueMap = new HashMap<>();

            String childUniqueKey = "";

            childUniqueKey = childName + "-" + childRev + "-" + childPosition;
            if (existingChildInfoRelMap.containsKey(childUniqueKey)) {
                existingChildInfoRelMap.remove(childUniqueKey);
            }

            try {

                childInfoIdMap = searchChildItem(context, childTNR, commonSearch, childUniqueKey, childInfoIdMap);
                childId = childInfoIdMap.get(childUniqueKey);

                propertyNameValueMap.put("organization", parentBusinessObject.getOrganizationOwner(context).getName());
                propertyNameValueMap.put("project", parentBusinessObject.getProjectOwner(context).getName());

                childInfo.setPropertyNameValueMap(propertyNameValueMap);
                childInfo.setChildInventoryUnit(childInfoIdMap.get("inventoryUnit"));
                childInfoIdMap.remove("inventoryUnit");

            } catch (NullPointerException | MatrixException exp) {
                HAGFORS_BOM_IMPORT_VALIDATOR_LOGGER.error(exp);
                throw exp;
            } catch (Exception exp) {
                HAGFORS_BOM_IMPORT_VALIDATOR_LOGGER.error(exp);
                throw exp;
            }

            String positionAttribute = "";
            try {

                List<Attribute> relAttributeList = null;
                for (int i = 0; i < relationshipList.size(); i++) {
                    Relationship rel = relationshipList.get(i);
                    if (rel.getFromType().equalsIgnoreCase(parentType) && rel.getToType().equalsIgnoreCase(childType)) {
                        relName = rel.getName();
                        interfaceName = rel.getInterfaces();
                        relAttributeList = rel.getAttributes().getAttributeList();
                        for (Attribute attribute : relAttributeList) {
                            if (attribute.getSourceName().equalsIgnoreCase("Position")) {
                                positionAttribute = attribute.getDestinationName();
                            }
                        }
                    }
                }

                Optional.ofNullable(relAttributeList).orElseThrow(() -> new RuntimeException(parentTNR.getName() + " has not been allowed as a parent type"));

                List relListWithQuantity = Arrays.asList(PropertyReader.getProperty("BOM.rel.with.quantity.att.list").split("\\|"));

                childQuantityValidation(relListWithQuantity, relName, childQuantity, childName, parentName);
                childPositionValidation(childPosition, parentName, duplicatePositionValidatorMap, childInfo);

                ArrayList<String> connectedChildRelIDlist = new ArrayList<>();
                HAGFORS_BOM_IMPORT_VALIDATOR_LOGGER.debug("Check connection for relationship name" + relName);
                try {
                    //relID = BusinessObjectUtil.checkToRelationship(context, parentID, childId, relName);
                    connectedChildRelIDlist = businessObjectUtil.checkToRelationshipWithPosition(context, parentObjectId, childId, relName, positionAttribute, childPosition);
                    HAGFORS_BOM_IMPORT_VALIDATOR_LOGGER.debug("Total connection found between " + parentName + " and Child " + childName + " is " + connectedChildRelIDlist.size());
                } catch (MatrixException ex) {
                    HAGFORS_BOM_IMPORT_VALIDATOR_LOGGER.error(ex);
                }

                HashMap<String, String> attributeNameValueMap = prepareAttributeNameValueMap(relAttributeList, lineDataMap, childName, parentName);
                childQuantityFloat = negativeQuantityValidation(lineDataMap, attributeNameValueMap, relListWithQuantity, relName, childQuantityFloat, childName, parentName);

                /*
                If a line contain same child with same position, then considering as same BOM. 
                 */
                if (childInfoMap.containsKey(childName + childPosition)) {
                    ChildInfo child = childInfoMap.get(childName + childPosition);
                    int previousQuantity = child.getChildQuantity();
                    childInfo.setChildQuantity(Math.round(childQuantityFloat) + previousQuantity);
                } else {
                    childInfo.setChildQuantity(Math.round(childQuantityFloat));
                }

                if (!NullOrEmptyChecker.isNullOrEmpty(connectedChildRelIDlist)) {
                    childInfo.setRelIDList(connectedChildRelIDlist);
                } else {
                    childInfo.setRelIDList(null);
                }
                childInfo.setRelName(relName);
                childInfo.setInterfaceName(interfaceName);
                childInfo.setParentId(parentObjectId);
                childInfo.setChildId(childId);

//                setUsageCoefficient(childQuantityFloat, childInfo, parentName, attributeNameValueMap);
                setUsageCoefficient(childQuantityFloat, childInfo, parentName, attributeNameValueMap, commonBomImportVariables.source);

                childInfo.setAttributeNameValueMap(attributeNameValueMap);

                childInfoMap.put(childName + childPosition, childInfo);
            } catch (RuntimeException exp) {
                HAGFORS_BOM_IMPORT_VALIDATOR_LOGGER.error(exp);
                throw exp;
            } catch (Exception exp) {
                HAGFORS_BOM_IMPORT_VALIDATOR_LOGGER.error(exp);
                throw exp;
            }
        }

        requestParentChildInfoMap.put(parentInfo, childInfoMap);
        dataCollector.setRequestParentChildInfoMap(requestParentChildInfoMap);
        dataCollector.setExistingChildInfoRelMap(existingChildInfoRelMap);

        HAGFORS_BOM_IMPORT_VALIDATOR_LOGGER.info("Lines validation process has been completed");
        return dataCollector;
    }

    // ##########################################################################
    // # Inventory Unit = PC/PCS/EMPTY/NULL                                     #
    // # Number of Instances = Quantity                                         #
    // ##########################################################################
    // ##########################################################################
    // # Inventory Unit = AREA/LENGTH                                           #
    // # IF (length /1000) * (width / 1000) * Number of Unit = Net Quantity THEN# 
    // # Number of Instances = Number Of Unit                                   #
    // # Usage Coefficient = (length /1000) * (width / 1000)                    #
    // # ELSE IF (length /1000) * (width / 1000) * Number of Unit = 0           #
    // # Number of Instances = 1                                                #
    // # Usage Coefficient = NetQuantity                                        #
    // ##########################################################################
    // ##########################################################################
    // # Inventory Unit = MASS/VOLUME                                           #
    // # Number of Instances = 1                                                #
    // # Usage Coefficient = NetQuantity                                        #
    // ##########################################################################
    /**
     * @param childQuantityFloat
     * @param childInfo
     * @param parentName
     * @param attributeNameValueMap
     */
    private void setUsageCoefficient(float childQuantityFloat, ChildInfo childInfo, String parentName, HashMap<String, String> attributeNameValueMap) {
        Optional.ofNullable(childInfo.getChildInventoryUnit()).filter(inventoryUnit -> !inventoryUnit.isEmpty()).ifPresent((String inventoryUnit) -> {
            CommonUtilities commonUtilities = new CommonUtilities();
            Double usageCoefficient = commonUtilities.getUsageCoefficient(childInfo, Double.valueOf(childQuantityFloat), parentName);
            attributeNameValueMap.put("ProcessInstanceContinuous.V_UsageContCoeff", usageCoefficient.toString());
        });
    }

    private void setUsageCoefficient(Float childQuantityFloat, ChildInfo childInfo, String parentName, HashMap<String, String> attributeNameValueMap, String source) {
        Optional.ofNullable(childInfo.getChildInventoryUnit()).filter(inventoryUnit -> !inventoryUnit.isEmpty()).ifPresent((String inventoryUnit) -> {
            String usageCoefficient = Boolean.parseBoolean(PropertyReader.getProperty("mopaz.calculated.usage.coefficient"))
                    ? new CommonUtilities().getUsageCoefficient(childInfo, Double.valueOf(childQuantityFloat), parentName, source).toString()
                    : setUsageCoefficient(childInfo, childQuantityFloat);
            attributeNameValueMap.put("ProcessInstanceContinuous.V_UsageContCoeff", usageCoefficient);
        });
    }

    private String setUsageCoefficient(ChildInfo childInfo, Float childQuantity) {
        childInfo.setChildQuantity(1);
        return childQuantity.toString();
    }

    private void childPositionValidation(String childPosition, String parentName, HashMap<String, String> duplicatePositionValidatorMap, ChildInfo childInfo) {
        String childName = childInfo.getChildTNR().getName();
        String childRev = childInfo.getChildTNR().getRevision();

        Optional.ofNullable(childPosition)
                .filter(position -> isPositiveInteger(position))
                .orElseThrow(() -> new RuntimeException(MessageFormat.format(PropertyReader.getProperty("BOM.position.validation"), "'" + childPosition + "'", "'" + childName + "'", "'" + parentName + "'")));

        if (duplicatePositionValidatorMap.containsKey(childPosition)) {
            childDuplicatePositionValidation(duplicatePositionValidatorMap, childPosition, childInfo, parentName);
        } else {
            duplicatePositionValidatorMap.put(childPosition, childName + "|" + childRev);
        }
    }

    private void childQuantityValidation(List relListWithQuantity, String relName, String childQuantity, String childName, String parentName) {
        if (relListWithQuantity.contains(relName)) {
            Optional.ofNullable(childQuantity)
                    .filter(quantity -> Double.parseDouble(quantity) != 0)
                    .orElseThrow(() -> new RuntimeException(MessageFormat.format(PropertyReader.getProperty("BOM.quantity.can.not.zero.or.fraction"), "'" + childName + "'", "'" + parentName + "'")));
        } else {

            Optional.ofNullable(childQuantity)
                    .filter(quantity -> Double.parseDouble(quantity) > 0)
                    .orElseThrow(() -> new RuntimeException(MessageFormat.format(PropertyReader.getProperty("BOM.non.material.item.quantity.can.not.be.negative"), "'" + childName + "'", "'" + parentName + "'")));

            Optional.ofNullable(childQuantity)
                    .filter((String quantity) -> Double.parseDouble(quantity) != 0 || Double.parseDouble(quantity) % 1 == 0)
                    .orElseThrow(() -> new RuntimeException(MessageFormat.format(PropertyReader.getProperty("BOM.quantity.can.not.zero.or.fraction"), "'" + childName + "'", "'" + parentName + "'")));
        }
    }

    private Float negativeQuantityValidation(HashMap<String, String> lineDataMap, HashMap<String, String> attributeNameValueMap, List relListWithQuantity, String relName, Float childQuantity, String childName, String parentName) {
        if (relListWithQuantity.contains(relName)) {
            if (childQuantity < 0) {
                attributeNameValueMap.put(PropertyReader.getProperty("attribute.material.items.by.product"), "true");
                childQuantity = childQuantity * (-1);
                lineDataMap.put("Net quantity", childQuantity.toString());
            }
        } else {
            if (childQuantity < 0) {
                String errorMessage = MessageFormat.format(PropertyReader.getProperty("BOM.non.material.item.quantity.can.not.be.negative"),
                        "'" + childName + "'",
                        "'" + parentName + "'");

                throw new RuntimeException(errorMessage);
            }
        }
        return childQuantity;
    }

    private void childDuplicatePositionValidation(HashMap<String, String> duplicatePositionValidatorMap, String childPosition, ChildInfo childInfo, String parentName) throws RuntimeException {
        String itemNameRev = duplicatePositionValidatorMap.get(childPosition);
        String childName = childInfo.getChildTNR().getName();
        String childRev = childInfo.getChildTNR().getRevision();

        if (!itemNameRev.equalsIgnoreCase(childName + "|" + childRev)) {
            String[] duplicateChildInfo = duplicatePositionValidatorMap.get(childPosition).split("\\|");
            String duplicateChildName = "";
            String duplicateChildRev = "";
            if (duplicateChildInfo != null) {
                duplicateChildName = duplicateChildInfo[0];
                if (duplicateChildInfo.length == 2) {
                    duplicateChildRev = duplicateChildInfo[1];
                }
            }
            HAGFORS_BOM_IMPORT_VALIDATOR_LOGGER.error("'" + childInfo.getChildTNR().getType() + "' '" + childInfo.getChildTNR().getName() + "' has duplicate position");
            String duplicateError = MessageFormat.format(PropertyReader.getProperty("duplicate.position.not.allowed"),
                    "'" + childPosition + "'",
                    "'" + childName + "'",
                    "'" + childRev + "'",
                    "'" + duplicateChildName + "'",
                    "'" + duplicateChildRev + "'",
                    "'" + parentName + "'");
            throw new RuntimeException(duplicateError);
        }
    }

    private HashMap<String, String> prepareAttributeNameValueMap(List<Attribute> relAttributeList, HashMap<String, String> lineDataMap, String childName, String parentName) {
        CommonUtilities commonUtilities = new CommonUtilities();
        HashMap<String, String> attributeNameValueMap = new HashMap<>();
        relAttributeList.stream().parallel().forEach((Attribute attribute) -> {
            attribute.setIsRequired(Optional.ofNullable(attribute.getIsRequired()).orElse(Boolean.FALSE));

            Optional.ofNullable(lineDataMap.get(attribute.getSourceName())).filter(attributeData -> !attributeData.isEmpty() && attribute.getIsRequired()).orElseThrow(() -> new RuntimeException(MessageFormat.format(PropertyReader.getProperty("missing.child.attribute.exception"),
                    "'" + attribute.getSourceName() + "'",
                    "'" + childName + "'",
                    "'" + parentName + "'")));

            String jsonAttributeValue = lineDataMap.get(attribute.getSourceName());

            attributeNameValueMap.put(attribute.getDestinationName(), commonUtilities.convertToRealValues(attribute, jsonAttributeValue));
        });

        return attributeNameValueMap;
    }

    private HashMap<String, String> searchChildItem(Context context, TNR childTNR, CommonSearch commonSearch, String childUniqueKey, HashMap<String, String> childInfoIdMap) throws Exception {
        String childId;
        if (!childInfoIdMap.containsKey(childUniqueKey)) {
            try {
                Instant start_child_find_time = Instant.now();

                List<String> selectDataList = new ArrayList<>();
                selectDataList.add("id");
                selectDataList.add("type");
                selectDataList.add("name");
                selectDataList.add("revision");
                String inventoryUnitAttribute = "attribute[" + PropertyReader.getProperty("import.item.val.component.material.attr.inventory.unit") + "].value";
                selectDataList.add(inventoryUnitAttribute);

                List<HashMap<String, String>> childItemSearchedAttributes = commonSearch.searchItem(context, childTNR, selectDataList);
                childId = childItemSearchedAttributes.get(0).get("id");

                Instant end_child_find_time = Instant.now();
                HAGFORS_BOM_IMPORT_VALIDATOR_LOGGER.info(" | Search | " + childTNR.getName() + " | " + DateTimeUtils.getDuration(start_child_find_time, end_child_find_time));

                childInfoIdMap.put(childUniqueKey, childId);
                childInfoIdMap.put("inventoryUnit", childItemSearchedAttributes.get(0).get(inventoryUnitAttribute));
            } catch (Exception ex) {
                HAGFORS_BOM_IMPORT_VALIDATOR_LOGGER.error(ex);
                throw ex;
            }
        }
        return childInfoIdMap;
    }

    private TNR parentItemTNRValidation(TNR parentItem) {
        TNR parentTNR = new TNR(
                Optional.ofNullable(parentItem.getType()).orElseThrow(() -> new RuntimeException("Type is missing for parent " + parentItem.getType() + " " + parentItem.getName() + " " + parentItem.getRevision())),
                Optional.ofNullable(parentItem.getName()).orElseThrow(() -> new RuntimeException("Name is missing for parent " + parentItem.getType() + " " + parentItem.getName() + " " + parentItem.getRevision())),
                Optional.ofNullable(parentItem.getRevision()).orElseThrow(() -> new RuntimeException("Revision is missing for parent " + parentItem.getType() + " " + parentItem.getName() + " " + parentItem.getRevision())));

        return parentTNR;
    }

    private BusinessObject searchParentItem(Context context, TNR parentTNR, CommonSearch commonSearch) throws MatrixException, Exception {
        try {
            Instant start_find_time = Instant.now();
            List<HashMap<String, String>> parentItemSearchedAttributes = commonSearch.searchItem(context, parentTNR);

            String parentBusinessObjectId = parentItemSearchedAttributes.get(0).get("id");
            BusinessObject parentBusinessObject = new BusinessObject(parentBusinessObjectId);

            Instant end_find_time = Instant.now();
            HAGFORS_BOM_IMPORT_VALIDATOR_LOGGER.info(" | Search | " + parentTNR.getName() + " | " + DateTimeUtils.getDuration(start_find_time, end_find_time));

            return parentBusinessObject;
        } catch (MatrixException ex) {
            HAGFORS_BOM_IMPORT_VALIDATOR_LOGGER.error(ex);
            throw ex;
        } catch (Exception ex) {
            HAGFORS_BOM_IMPORT_VALIDATOR_LOGGER.error(ex);
            throw ex;
        }
    }

    public String validateChildAttirbutesInTheRequest(String requestAttirbute, HashMap<String, String> lineDataMap, TNR tempChildTNR, TNR parentTNR) {
        return Optional.ofNullable(lineDataMap.get(requestAttirbute)).filter(mapKeyData -> !mapKeyData.isEmpty()).orElseThrow(() -> new RuntimeException("'" + requestAttirbute + "' can't be null or empty for " + tempChildTNR.toString() + " object under parent " + parentTNR));
    }

    public final boolean isPositiveInteger(String s) {
        return POSITIVE_INTEGER_PATTERN.matcher(s).matches();
    }
}
