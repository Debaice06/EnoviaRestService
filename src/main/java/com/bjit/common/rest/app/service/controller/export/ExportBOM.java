package com.bjit.common.rest.app.service.controller.export;

import com.bjit.common.code.utility.security.ContextPasswordSecurity;
import com.bjit.common.rest.app.service.background.processors.BackgroundProcessor;
import com.bjit.common.rest.app.service.background.processors.BackgroundResponse;
import com.bjit.common.rest.app.service.background.processors.BackgroundRunnable;
import com.bjit.common.rest.app.service.bomExport.BomExportUtil;
import com.bjit.common.rest.app.service.context.CreateContext;
import com.bjit.common.rest.app.service.controller.authentication.AuthenticationUserModel;
import com.bjit.common.rest.app.service.background.rnp.RnPBackgroundProcessImpl;
import com.bjit.common.rest.app.service.background.rnp.RnPBackgroundProcessSingleLevelImpl;
import com.bjit.common.rest.app.service.background.rnp.RnPBomData;
import com.bjit.common.rest.app.service.payload.common_response.CustomResponseBuilder;
import com.bjit.common.rest.app.service.payload.common_response.IResponse;
import com.bjit.common.rest.app.service.utilities.NullOrEmptyChecker;
import com.bjit.common.rest.app.service.utilities.ObjectMapper;
import com.bjit.common.rest.app.service.utilities.VerifyToken;
import com.bjit.ewc18x.model.AttributesForm;
import com.bjit.ewc18x.model.ResponseForReport;
import com.bjit.ewc18x.model.Status;
import com.bjit.ewc18x.service.ExpandObjectServiceImpl;
import com.bjit.ewc18x.utils.CustomException;
import com.bjit.ewc18x.utils.EnoviaWebserviceCommon;
import com.bjit.ewc18x.utils.PropertyReader;
import com.bjit.ewc18x.validator.BOMExportValidation;
import com.bjit.plmkey.ws.controller.expandobject.ExpandObjectUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import matrix.db.Context;
import org.apache.commons.io.IOUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.bjit.ewc18x.model.ExpandObjectForm;
import com.bjit.ewc18x.service.ExpandObjectService;
import com.bjit.ewc18x.utils.MqlQueries;
import com.bjit.mapper.mapproject.jasper_report.JasperReportGenerator;
import com.bjit.mapper.mapproject.util.CommonUtil;
import com.bjit.mapper.mapproject.util.Constants;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import matrix.db.BusinessObject;
import matrix.util.MatrixException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.xml.sax.SAXException;
import com.bjit.common.rest.app.service.controller.export.himelli.HimelliLogger;
import com.bjit.common.rest.app.service.controller.export.himelli.HimelliReportUtility;
import com.bjit.common.rest.app.service.controller.export.himelli.HimelliReportProcessor;
import com.bjit.common.rest.app.service.controller.export.himelli.LogType;
import com.bjit.common.rest.app.service.controller.export.himelli.MultiLevelBOMStructureUtils;
import com.bjit.common.rest.app.service.controller.export.report.single_level.model.ReportBusinessModel;
import com.bjit.common.rest.app.service.controller.export.report.single_level.model.ReportDataModel;
import com.bjit.common.rest.app.service.controller.export.report.single_level.model.ReportParameterModel;
import com.bjit.common.rest.app.service.controller.export.report.single_level.provider.ContextProvider;
import com.bjit.common.rest.app.service.controller.export.report.single_level.provider.ReportDataProvider;
import com.bjit.common.rest.app.service.controller.export.report.single_level.validator.ReportParameterValidator;
import com.bjit.common.rest.app.service.export.product.ProductExportService;
import com.bjit.common.rest.app.service.search.SearchService;
import com.bjit.common.rest.pdm_enovia.bom.comparison.constant.Constant;
import com.bjit.common.rest.app.service.controller.lntransfer.LNTransferService;
import com.bjit.common.rest.app.service.controller.lntransfer.LNTransferServiceImpl;
import com.bjit.common.rest.app.service.controller.lntransfer.Util.LNRequestUtil;
import com.bjit.common.rest.app.service.background.rnp.RnPResponseHandler;
import com.bjit.common.rest.app.service.controller.export.himelli.HimelliRequestValidator;
import com.bjit.common.rest.app.service.model.itemTransfer.LNTransferRequestModel;
import com.bjit.common.rest.app.service.model.rnp.RnPModel;
import com.bjit.common.rest.app.service.payload.common_response.CommonResponse;
import com.bjit.common.rest.app.service.utilities.JSON;
import com.bjit.common.rest.app.service.utilities.ServiceRequester;
import com.bjit.common.rest.item_bom_import.xml_mapping_model.ResponseMessageFormaterBean;
import com.bjit.ex.integration.transfer.actions.GTSNightlyUpdateTransferAction;
import com.bjit.ex.integration.transfer.actions.LNTransferAction;
import com.bjit.mapper.mapproject.expand.ObjectTypesAndRelations;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MqlUtil;
import java.io.FileNotFoundException;
import com.bjit.common.rest.app.service.utilities.DateTimeUtils;
import com.bjit.common.rest.item_bom_import.xml_mapping_model.ResponseMessageFormaterBean;
import com.bjit.dw.enovia.action.EnoviaNightlyUpdateAction;
import com.bjit.ex.integration.transfer.actions.GTSNightlyUpdateTransferAction;
import com.bjit.ex.integration.transfer.actions.LNTransferAction;
import com.bjit.mapper.mapproject.jsonOutput.SelectedAtrributeFetch;
import java.net.ConnectException;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Date;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.StringJoiner;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(path = "/export")
public class ExportBOM {

    @Lazy
    @Autowired
    RnPResponseHandler rnpResponseHandler;

    String token;
    AuthenticationUserModel userCredentialsModel;
    private static HashMap<String, String> DIRECTORY_MAP;
    private static final org.apache.log4j.Logger EXPORT_BOM_LOGGER = org.apache.log4j.Logger.getLogger(ExportBOM.class);
    private final static String BOM_EXPORT_CONFIG_FILE_NAME = "attributeConf";
    @Autowired
    private ProductExportService productExportService;
    String json = "";
    @Autowired
    private SearchService searchService;

    @ResponseBody
    @RequestMapping(value = "/{serviceName}", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public ResponseEntity ExportBOM(HttpServletRequest httpRequest, @PathVariable String serviceName,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "name", required = false) String name, // code level validation for this parameter
            @RequestParam(value = "rev", required = false) String rev,
            @RequestParam(value = "attrs", required = false) String attributes, // code level validation for this parameter
            @RequestParam(value = "reportName", required = false) String reportName,
            @RequestParam(value = "requestId", required = false) String requestId,
            @RequestParam(value = "format", required = false) String format,
            @RequestParam(value = "includeFiles", required = false) boolean includeFiles
    ) {
        BOMExportValidation bomExportValidation = new BOMExportValidation();
        Map<String, String> userCredentials = null;
        String outputData = "";
        String user = "";
        String pass = "";
        File serviceFile;
        Context context = null;
        IResponse responseBuilder = new CustomResponseBuilder();
        try {
            ExpandObjectUtil.addGraphicsSupport(); // necessary for report generation in different environment
            requestId = bomExportValidation.generateRequestId(requestId);
            userCredentials = bomExportValidation.getUserCredentials(httpRequest);
            EXPORT_BOM_LOGGER.info("Validating credentials and services and also generates service file");
            serviceFile = validateServiceRequestAndGetFile(httpRequest, serviceName, attributes);
            type = bomExportValidation.getType(type);
            reportName = bomExportValidation.getReportName(reportName);
            EnoviaWebserviceCommon enoviaWebserviceCommon = new EnoviaWebserviceCommon();
            EXPORT_BOM_LOGGER.info("Generating context");
            user = userCredentials.get("user");
            pass = userCredentials.get("pass");
            context = enoviaWebserviceCommon.getSecureContext(user, pass);
            outputData = getDataFromExpandObjectService(context, user, pass, serviceFile, requestId, attributes, serviceName, type, name, rev, reportName, format, includeFiles);

            //String buildResponse = responseBuilder.setData(outputData).setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.OK).buildResponse(true, true);
            //return new ResponseEntity<>(buildResponse, HttpStatus.OK);
            return new ResponseEntity<>(outputData, HttpStatus.OK);
            //return outputData;
        } catch (Exception ex) {
            String buildResponse = responseBuilder.addErrorMessage(ex.getMessage()).setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.FAILED).buildResponse();
            return new ResponseEntity<>(buildResponse, HttpStatus.EXPECTATION_FAILED);
            //return new Response(name, Status.FAILED, requestId, ex.getMessage());
        }
    }

    @ResponseBody
    @RequestMapping(value = "TEBom/{serviceName}", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public ResponseEntity TExportBOM(HttpServletRequest httpRequest, @PathVariable String serviceName,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "name", required = false) String name, // code level validation for this parameter
            @RequestParam(value = "rev", required = false) String rev,
            @RequestParam(value = "attrs", required = false) String attributes, // code level validation for this parameter
            @RequestParam(value = "reportName", required = false) String reportName,
            @RequestParam(value = "requestId", required = false) String requestId,
            @RequestParam(value = "format", required = false) String format,
            @RequestParam(value = "includeFiles", required = false) boolean includeFiles
    ) throws Exception {
        IResponse responseBuilder = new CustomResponseBuilder();
        this.token = httpRequest.getHeader("token");

        try {
            getUserCredentials(httpRequest);
        } catch (Exception exp) {
            String buildResponse = responseBuilder.addErrorMessage(exp.getMessage()).setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.FAILED).buildResponse();
            return new ResponseEntity<>(buildResponse, HttpStatus.EXPECTATION_FAILED);
        }
        BOMExportValidation bomExportValidation = new BOMExportValidation();
        Map<String, String> userCredentials = null;
        String outputData = "";
        String user = userCredentialsModel.getUserId();
        String pass = userCredentialsModel.getPassword();
        File serviceFile;
        Context context = null;
        try {
            ExpandObjectUtil.addGraphicsSupport(); // necessary for report generation in different environment
            requestId = bomExportValidation.generateRequestId(requestId);
            userCredentials = bomExportValidation.getUserCredentials(user, pass);
            EXPORT_BOM_LOGGER.info("Validating credentials and services and also generates service file");
            serviceFile = validateServiceRequestAndGetFile(httpRequest, serviceName, attributes);
            type = bomExportValidation.getType(type);
            reportName = bomExportValidation.getReportName(reportName);
            EnoviaWebserviceCommon enoviaWebserviceCommon = new EnoviaWebserviceCommon();
            EXPORT_BOM_LOGGER.info("Generating context");
            context = enoviaWebserviceCommon.getSecureContext(user, pass);
            outputData = getDataFromExpandObjectService(context, user, pass, serviceFile, requestId, attributes, serviceName, type, name, rev, reportName, format, includeFiles);
            String buildResponse = responseBuilder.setData(outputData).setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.OK).buildResponse();
            return new ResponseEntity<>(buildResponse, addTokenToResponse(), HttpStatus.OK);
        } catch (Exception exp) {
            String buildResponse = responseBuilder.addErrorMessage(exp.getMessage()).setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.FAILED).buildResponse();
            return new ResponseEntity<>(buildResponse, addTokenToResponse(), HttpStatus.EXPECTATION_FAILED);
        }
    }

    private File validateServiceRequestAndGetFile(HttpServletRequest httpRequest, String serviceName, String attributes) throws Exception {
        try {
            BOMExportValidation bomExportValidation = new BOMExportValidation();
            EXPORT_BOM_LOGGER.info("Validating request");
            Boolean isRequestValid = bomExportValidation.validateBOMExportServiceRequest(httpRequest, serviceName, attributes);
            if (!isRequestValid) {
                EXPORT_BOM_LOGGER.info("Request is invalid");
                throw new Exception(Constants.ATTRIBUTION_EXCEPTION);
            }
            EXPORT_BOM_LOGGER.info("Getting service file");
            EXPORT_BOM_LOGGER.debug("Service name : " + serviceName);
            return bomExportValidation.getServiceFile(serviceName);
        } catch (Exception exp) {
            EXPORT_BOM_LOGGER.error(exp.getMessage());
            throw exp;
        }
    }

    private ExpandObjectForm setDataIntoExpandForm(Context context, String user, String pass, File file, String requestId, String attributes, String serviceName, String type, String name, String rev) throws CustomException, Exception {
        EXPORT_BOM_LOGGER.info("Setting data into 'ExpandObjectForm'");
        ExpandObjectForm expandObjectForm = new ExpandObjectForm();
        expandObjectForm.setUserID(user);
        expandObjectForm.setPassword(pass);
        expandObjectForm.setDefaultTypeList(ExpandObjectUtil.getExpandObjectTypeList(file));
        expandObjectForm.setSelectedTypeList(ExpandObjectUtil.getExpandObjectTypeList(file));
        try {
            EXPORT_BOM_LOGGER.info("Getting revision");
            rev = ExpandObjectUtil.getRev(context, requestId, type, name, rev);
            EXPORT_BOM_LOGGER.debug("Revision : " + rev);
        } catch (Exception exp) {
            EXPORT_BOM_LOGGER.error(exp.getMessage());
            throw exp;
        }
        expandObjectForm.setName(name);
        expandObjectForm.setType(type);
        expandObjectForm.setRevision(rev);
        expandObjectForm.setRecursionLevel(0);
        expandObjectForm.setServiceName(serviceName);
        expandObjectForm.setGetFrom(Boolean.TRUE);
        expandObjectForm.setGetTo(Boolean.FALSE);

        try {
            expandObjectForm.setSelectedItem((ArrayList<String>) ExpandObjectUtil.getAttributeList(attributes));
        } catch (Exception exp) {
            EXPORT_BOM_LOGGER.error(exp.getMessage());
            throw exp;
        }

        return expandObjectForm;
    }

    /**
     * this method using Web Service for object expansion
     *
     * @param
     * @throws
     * @return
     */
    private String getDataFromExpandObjectService(Context context, String user, String pass, File serviceFile, String requestId, String attrs, String serviceName, String type, String name, String rev, String reportName, String format, boolean includeFiles) throws CustomException, Exception {
        ExpandObjectService expandObjectService = new ExpandObjectServiceImpl();
        AttributesForm attributeForm = new AttributesForm();
        // expand object using the matrix API finish 
        ExpandObjectForm expandObjectForm = setDataIntoExpandForm(context, user, pass, serviceFile, requestId, attrs, serviceName, type, name, rev);
        EXPORT_BOM_LOGGER.debug("Reading data from 'Bom Export Config File'. File name is : " + BOM_EXPORT_CONFIG_FILE_NAME);
        attributeForm.readValues(ExpandObjectUtil.getConfigFileNameFromService(serviceFile, BOM_EXPORT_CONFIG_FILE_NAME));
        Map<String, String> allItemMap = attributeForm.getAttributeNameMap();
        EXPORT_BOM_LOGGER.debug("All item map : " + allItemMap);
        List<String> allRelAttributeList = (ArrayList<String>) attributeForm.getRelationshipAttrName();
        EXPORT_BOM_LOGGER.debug("All relation attribute List : " + allRelAttributeList);
        List<String> propertyList = (ArrayList<String>) attributeForm.getPropertyNames();
        EXPORT_BOM_LOGGER.debug("Property List : " + propertyList);
        List<String> notPropertyNotAttributeList = (ArrayList<String>) attributeForm.getNotPropertyNotAttributeNames();
        EXPORT_BOM_LOGGER.debug("Not property not attribute list : " + notPropertyNotAttributeList);
        List<String> selectedAttrList = expandObjectForm.getSelectedItem();
        EXPORT_BOM_LOGGER.debug("Selected attribute list : " + selectedAttrList);
        EXPORT_BOM_LOGGER.info("Getting final selected attribute list");
        ExpandObjectUtil.getFinalSelectedAttributeList(selectedAttrList, propertyList, notPropertyNotAttributeList, allRelAttributeList, allItemMap);
        ExpandObjectUtil.finalSelectedObjParamList.addAll(ExpandObjectUtil.finalSelectedAttributeList);
        try {
            EXPORT_BOM_LOGGER.info("Populating 'ExpandObjectForm' from service");
            EXPORT_BOM_LOGGER.debug("Service name : " + serviceName);
            expandObjectForm = expandObjectService.populateServiceInfo(expandObjectForm, "services/" + serviceName);
        } catch (CustomException exp) {
            EXPORT_BOM_LOGGER.error(exp.getMessage());
            throw exp;
        }

        String physicalId;
        try {
            physicalId = ExpandObjectUtil.getPhycialId(context, expandObjectForm);
            EXPORT_BOM_LOGGER.debug("Physicial Id is : " + physicalId);
        } catch (CustomException exp) {
            EXPORT_BOM_LOGGER.error(exp.getMessage());
            throw exp;
        }

        String outputData;
        try {
            outputData = expandObjectService.getJsonOutput(null, context, physicalId, ExpandObjectUtil.finalSelectedObjParamList, ExpandObjectUtil.finalSelectedAttributeList, ExpandObjectUtil.finalSelectedRelAttributeList, expandObjectForm);
            EXPORT_BOM_LOGGER.debug("Output data : " + outputData);
        } catch (CustomException exp) {
            EXPORT_BOM_LOGGER.error(exp.getMessage());
            throw exp;
        }

        Map<String, String> replaceJSONMapString = ExpandObjectUtil.replaceJSONMapString;
        for (Map.Entry<String, String> jsonEntry : replaceJSONMapString.entrySet()) {
            outputData = outputData.replace(jsonEntry.getValue(), jsonEntry.getKey());
        }

        EXPORT_BOM_LOGGER.debug("Output Data is " + outputData);

        if (format != null && !outputData.isEmpty()) {
            String reportString = ExpandObjectUtil.generateReport(outputData, name, reportName, requestId, format);
            EXPORT_BOM_LOGGER.debug("Report is : " + reportString);
            if (!reportString.isEmpty()) {
                EXPORT_BOM_LOGGER.info("Valid report");
                ResponseForReport responseReport = new ResponseForReport(name, Status.OK, requestId, reportString);
                String reportResponseJSON = new Gson().toJson(responseReport);
                return reportResponseJSON;
            }
        }

        return outputData;
    }

    @ResponseBody
    @RequestMapping(value = "/download", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] downloadReport(
            @RequestParam(value = "requestId", required = true) String requestId,
            @RequestParam(value = "format", required = true) String format,
            HttpServletResponse response) {
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(PropertyReader.getProperty("ebom.reports.folder.path")))) {
            for (Path path : directoryStream) {
                if (path.toString().contains(requestId) && path.toString().endsWith(format)) {
                    String applicationType;
                    String probeContentType = Files.probeContentType(path);
                    if (probeContentType == null || probeContentType.equalsIgnoreCase("")) {
                        applicationType = "application/octet-stream";
                    } else {
                        applicationType = probeContentType;
                    }
                    response.setContentType(applicationType);
                    response.setHeader("Content-Disposition", "attachment; filename=\"" + path.toFile().getName() + "\"");
                    InputStream inputStream = new FileInputStream(path.toFile());
                    return IOUtils.toByteArray(inputStream);
                }
            }
        } catch (IOException ex) {
            return new ResponseForReport("", Status.FAILED, requestId, ex.getMessage()).toString().getBytes();
        } catch (Exception ex) {
            Logger.getLogger(ExportBOM.class.getName()).log(Level.SEVERE, null, ex);
            return new ResponseForReport("", Status.FAILED, requestId, ex.getMessage()).toString().getBytes();
        }
        return null;
    }

    /**
     * @param httpRequest
     * @param response
     * @param type
     * @param name
     * @param rev
     * @param latest
     * @param objectId
     * @param format
     * @param lang
     * @param primaryLang
     * @param secondaryLang
     * @param requestId
     * @param expandLevel
     * @param requester
     * @param attributeString
     * @param download
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/himelli/bom", method = RequestMethod.GET, produces = {MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public HttpEntity<ByteArrayResource> himelliBOMExport(HttpServletRequest httpRequest, HttpServletResponse response,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "name", required = false) String name, // code level validation for this parameter
            @RequestParam(value = "rev", required = false) String rev,
            @RequestParam(value = "isLatest", required = false, defaultValue = "false") boolean latest,
            @RequestParam(value = "objectId", required = false) String objectId,
            @RequestParam(value = "format", required = false, defaultValue = "json") String format,
            @RequestParam(value = "lang", required = false, defaultValue = "En") String lang,
            @RequestParam(value = "primaryLang", required = false) String primaryLang,
            @RequestParam(value = "secondaryLang", required = false) String secondaryLang,
            @RequestParam(value = "requestId", required = false) String requestId,
            @RequestParam(value = "expandLevel", required = false) String expandLevel,
            @RequestParam(value = "requester", required = false) String requester,
            @RequestParam(value = "printDrawing", required = false) String isDrawingInfoRequired,
            @RequestParam(value = "attrs", required = false) String attributeString,
            @RequestParam(value = "download", required = false, defaultValue = "true") boolean download,
            @RequestParam(value = "mainProjTitle", required = false) String mainProjTitle,
            @RequestParam(value = "psk", required = false) String psk,
            @RequestParam(value = "subTitle", required = false) String subTitle,
            @RequestParam(value = "product", required = false) String product,
            @RequestParam(value = "printDelivery", required = false) String printDelivery,
            @RequestParam(value = "treeView", required = false) String treeView,
            @RequestParam(value = "drawingNumber", required = false) String drawingNumber,
            @RequestParam(value = "drawingType", required = false) String docType
    ) {
        IResponse responseBuilder = new CustomResponseBuilder();
        String buildResponse = "";
        String errorMessage = "";
        String himelliReportName = new HimelliReportUtility().generateHimelliReportName(name, rev);

        //as Unique key is mandatory attribute , adding it manually 
        attributeString = attributeString + ",Unique Key";
        try {
            //validating Himelli request params....
            HimelliRequestValidator requestValidator = new HimelliRequestValidator();
            boolean isValidRequest;

            isValidRequest = requestValidator.validateHimelliServiceRequest(httpRequest, attributeString);

            if (!isValidRequest) {
                buildResponse = responseBuilder.setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.FAILED).addErrorMessage(Constants.ATTRIBUTION_EXCEPTION).setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.FAILED).buildResponse();
                EXPORT_BOM_LOGGER.error(buildResponse);
                return new HttpEntity<>(new ByteArrayResource(buildResponse.getBytes()));
            }
        } catch (Exception ex) {
            Logger.getLogger(ExportBOM.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            /*
             * String user = httpRequest.getHeader("user"); String pass =
             * httpRequest.getHeader("pass"); httpRequest.setAttribute("user",
             * Base64.getDecoder().decode(user)); httpRequest.setAttribute("pass",
             * Base64.getDecoder().decode(pass));
             */
            treeView = "true";
            ResponseEntity<?> valconBomJson = (ResponseEntity<?>) generateBomReport(httpRequest, response, type, name, rev,
                    latest, objectId, "json", lang, primaryLang, secondaryLang, requestId, expandLevel, requester, isDrawingInfoRequired,
                    attributeString, download, mainProjTitle, psk, subTitle, product, printDelivery, treeView, docType);
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(valconBomJson.getBody().toString());

            Object objStatus = json.get("status");
            String status = objStatus.toString();

            if (!(status.equals("OK"))) {
                Object Message = json.get("systemErrors");
                errorMessage = Message.toString();
                errorMessage = errorMessage.replaceAll("[\\\"]", " ");
            }

            HimelliLogger.getInstance().printLog("response body: " + valconBomJson.getBody(), LogType.INFO);
            HimelliLogger.getInstance().printLog("valcon response object: " + valconBomJson.toString(), LogType.INFO);

            MultiLevelBOMStructureUtils multiLevelBOMStructureUtilsObj = new MultiLevelBOMStructureUtils(json, true);
            json = multiLevelBOMStructureUtilsObj.modifyDataForHimeliExport();
            HimelliLogger.getInstance().printLog("mod treeView: " + json, LogType.INFO);
            HimelliReportProcessor himelliReportProcessing = new HimelliReportProcessor(json);
            // drawing number is not included in request parameter or is set to false, not include in himelli export
            if (NullOrEmptyChecker.isNullOrEmpty(drawingNumber) || !Boolean.parseBoolean(drawingNumber)) {
                himelliReportProcessing.addToNotIncludedAttributes("Dwg", "Drawing Number");
            }
            byte[] himelliReport = himelliReportProcessing.himelliDataProcessing();
            HttpHeaders header = new HttpHeaders();
            header.set(HttpHeaders.CONTENT_TYPE, "text/plain");
            header.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + himelliReportName);
            return new HttpEntity<>(new ByteArrayResource(himelliReport), header);
        } catch (Exception e) {
            HttpHeaders header = new HttpHeaders();
            header.set(HttpHeaders.CONTENT_TYPE, "application/json");
            if (NullOrEmptyChecker.isNullOrEmpty(errorMessage)) {
                errorMessage = Constants.CONTEXT_EXCEPTION;
            }
            EXPORT_BOM_LOGGER.error(errorMessage);
            buildResponse = responseBuilder.addErrorMessage(errorMessage + "; Could not generate himelli format data.")
                    .setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.FAILED).buildResponse();
            return new HttpEntity<>(new ByteArrayResource(buildResponse.getBytes()), header);
        }
    }

    @ResponseBody
    @GetMapping(value = "/multiLevelBomDataTest", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PDF_VALUE})
    public Object generateBomDataReport(HttpServletRequest httpRequest, HttpServletResponse response,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "format", required = false) String format,
            @RequestParam(value = "lang", required = false) String lang,
            @RequestParam(value = "download", required = false) boolean download
    ) throws IOException {
        if (format == null) {
            format = Constants.PDF;
        }
        if (lang == null) {
            lang = Constants.ENGLISH;
        }
        if (NullOrEmptyChecker.isNullOrEmpty(type)) {
            type = Constant.DEFAULT_TYPE;
        }
        Resource resource;
        File file;
        String data = "";
        try {
            resource = new ClassPathResource(PropertyReader.getProperty("test.json.files.path." + type + "_" + name));
            file = resource.getFile();
            data = new Scanner(file).useDelimiter("\\Z").next();
        } catch (IllegalArgumentException ile) {
            return new ResponseEntity<>(Constants.OBJECT_EXCEPTION, HttpStatus.NOT_ACCEPTABLE);
        }
        if (format.equalsIgnoreCase(Constants.JSON)) {
            response.setHeader("Content-Type", "application/json; charset=UTF-8");
            httpRequest.setAttribute("Content-Type", "application/json; charset=UTF-8");
            httpRequest.setAttribute("Accept", "application/json; charset=UTF-8");
            return new ResponseEntity<>(data, HttpStatus.OK);
        }
        String jsonString = CommonUtil.getResultsFromBomDataResponse(data);
        if (!jsonString.isEmpty()) {
            JasperReportGenerator jasperReportGenerator = new JasperReportGenerator();
            try {
                HashMap rootItemParams = CommonUtil.getInfoMapFromBomDataResponse(data, "rootItemInfo");
                EXPORT_BOM_LOGGER.debug("Response of Root param: " + rootItemParams);
                Map<String, String> responseData = jasperReportGenerator.generateReport(jsonString, rootItemParams, null, format, lang, "", type);
                String outputFile = responseData.get("filePath");
                if (!outputFile.isEmpty()) {
                    EXPORT_BOM_LOGGER.debug("Output File: " + outputFile);
                    EXPORT_BOM_LOGGER.debug("Successfully done.");
                    String downloadLink = CommonUtil.generateReportDownloadLinks(httpRequest, responseData.get("fileId"), format);
                    EXPORT_BOM_LOGGER.debug("Report Download Link: " + downloadLink);
                    if (download) {
                        Path path = Paths.get(outputFile);
                        String applicationType;
                        String probeContentType = Files.probeContentType(path);
                        if (probeContentType == null || probeContentType.equalsIgnoreCase("")) {
                            applicationType = "application/octet-stream";
                        } else {
                            applicationType = probeContentType;
                        }
                        response.setContentType(applicationType);
                        response.setHeader("Content-Disposition", "attachment; filename=\"" + path.toFile().getName() + "\"");
                        InputStream inputStream = new FileInputStream(path.toFile());
                        return IOUtils.toByteArray(inputStream);
                    }
                } else {
                    EXPORT_BOM_LOGGER.debug("Failed to generate report file.");
                }
            } catch (Exception exp) {
                EXPORT_BOM_LOGGER.error(exp.getMessage());
            } finally {
                jasperReportGenerator = null;
            }
        }
        return null;
    }

    @GetMapping("/bomDataReport/download/{reportName}")
    public Object downloadRnPReport(HttpServletResponse response, @PathVariable String reportName) throws Exception {
        try {
            return rnpResponseHandler.getDownloadableReport(reportName, response);
        } catch (Exception exp) {
            EXPORT_BOM_LOGGER.error(exp);
            return rnpResponseHandler.getErrorResponse(exp);
        }
    }

    @ResponseBody
    //@GetMapping(value = "/multiLevelBomDataReport", produces = {MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PDF_VALUE})
    @GetMapping(value = "/multiLevelBomDataReport")
    public Object generateBomDataReport(HttpServletRequest httpRequest, HttpServletResponse httpServletResponse,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "name", required = false) String name, // code level validation for this parameter
            @RequestParam(value = "rev", required = false) String rev,
            @RequestParam(value = "objectId", required = false) String objectId,
            @RequestParam(value = "format", required = false) String format,
            @RequestParam(value = "lang", required = false) String lang,
            @RequestParam(value = "primaryLang", required = false) String primaryLang,
            @RequestParam(value = "secondaryLang", required = false) String secondaryLang,
            @RequestParam(value = "requestId", required = false) String requestId,
            @RequestParam(value = "expandLevel", required = false) String expandLevel,
            @RequestParam(value = "printDrawing", required = false) String isDrawingInfoRequired,
            @RequestParam(value = "printSummary", required = false) String isSummaryRequired,
            @RequestParam(value = "attrs", required = false) String attributeString,
            @RequestParam(value = "mainProjTitle", required = false) String mainProjTitle,
            @RequestParam(value = "psk", required = false) String psk,
            @RequestParam(value = "subTitle", required = false) String subTitle,
            @RequestParam(value = "product", required = false) String product,
            @RequestParam(value = "printDelivery", required = false) String printDelivery,
            @RequestParam(value = "download", required = false) boolean download,
            @RequestParam(value = "docType", required = false) String docType,
            @RequestParam(value = "receiverEmail", required = false) String receiverEmail
    ) throws IOException {
        EXPORT_BOM_LOGGER.info("Url parameters(For Report): Type: " + type + " ,Name: " + name
                + " ,Format: " + format + " ,Lang: " + lang + " ,Download: " + download + " ,RequestId: " + requestId + ", Expand Level: " + expandLevel);
        if (format == null) {
            format = Constants.PDF;
        }
        if (NullOrEmptyChecker.isNullOrEmpty(type)) {
            type = Constant.DEFAULT_TYPE;
        }
        if (lang == null) {
            lang = Constants.ENGLISH;
        }
        if (primaryLang == null) {
            primaryLang = Constants.ENGLISH;
        }
        if (requestId == null) {
            requestId = "";
        }
        Context context = null;

        boolean isMBOMReport = false;
        if (type.equalsIgnoreCase(Constant.DEFAULT_TYPE) || type.equals("CreateAssembly") || type.equals("ProcessContinuousCreateMaterial")) {
            isMBOMReport = true;
        }

        RnPResponseHandler responseHandler = new RnPResponseHandler();

        try {
            try {
                CreateContext createContext = new CreateContext();

                context = createContext.getAdminContext();
                if (!context.isConnected()) {
                    throw new Exception(Constants.CONTEXT_EXCEPTION);
                }
            } catch (Exception exp) {
                return new ResponseEntity<>(Constants.CONTEXT_EXCEPTION, HttpStatus.NOT_ACCEPTABLE);
            }

            RnPModel rnpModel = new RnPModel();
            rnpModel.setHttpRequest(httpRequest);
            rnpModel.setHttpResponse(httpServletResponse);
            rnpModel.setType(type);
            rnpModel.setName(name);
            rnpModel.setRev(rev);
            rnpModel.setObjectId(objectId);
            rnpModel.setFormat(format);
            rnpModel.setLang(lang);
            rnpModel.setPrimaryLang(primaryLang);
            rnpModel.setSecondaryLang(secondaryLang);
            rnpModel.setRequestId(requestId);
            rnpModel.setExpandLevel(expandLevel);
            rnpModel.setIsDrawingInfoRequired(isDrawingInfoRequired);
            rnpModel.setIsSummaryRequired(isSummaryRequired);
            rnpModel.setAttributeListString(attributeString);
            rnpModel.setMainProjTitle(mainProjTitle);
            rnpModel.setPsk(psk);
            rnpModel.setSubTitle(subTitle);
            rnpModel.setProduct(product);
            rnpModel.setPrintDelivery(printDelivery);
            rnpModel.setDownload(download);
            rnpModel.setContext(context);
            rnpModel.setBaseUrl(ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString());
            rnpModel.setReceiverEmail(receiverEmail);
            rnpModel.setDocType(docType);
            rnpModel.setIsMBOMReport(isMBOMReport);

            Boolean isBackgroundProcess = isBackgroundProcess(rnpModel);

            if (isBackgroundProcess && NullOrEmptyChecker.isNullOrEmpty(receiverEmail)) {
                throw new NullPointerException(type + " " + name + " " + rev + " is a large structure. Need report receiver mail address");
            }

            BackgroundResponse backgroundResponse = null;

            if (isBackgroundProcess) {
                backgroundResponse = setInBackground(rnpModel);
            } else {
                RnPBackgroundProcessImpl rnpBackgroundProcessImpl = new RnPBackgroundProcessImpl(rnpModel);
                BackgroundProcessor<File> backgroundProcessor = new BackgroundProcessor<>();
                backgroundResponse = backgroundProcessor.backGroundProcess(rnpBackgroundProcessImpl, isBackgroundProcess);
            }

            return responseHandler.getResponse(isBackgroundProcess, rnpModel);

        } catch (Exception exp) {
            EXPORT_BOM_LOGGER.error("Report generation error: " + exp.getMessage());
            return responseHandler.getErrorResponse(exp);
        } finally {
            if (context != null) {
                context.close();
                context = null;
            }
        }

    }
//
//    private BackgroundResponse setInBackground(RnPModel rnpModel) throws Exception {
//        BackgroundResponse backgroundResponse;
//
//        Context context = rnpModel.getContext();
//        HttpServletRequest httpRequest = rnpModel.getHttpRequest();
//        HttpServletResponse httpServletResponse = rnpModel.getHttpResponse();
//
//        removeContextAndHttpRequestAndResponse(rnpModel);
//        JSON json = new JSON();
//        String serilizedRnPModel = json.serialize(rnpModel);
//        backgroundResponse = syncServiceCall(PropertyReader.getProperty("rnp.background.process.message.queue.url"), serilizedRnPModel);
//
//        setContextAndHttpRequestAndResponse(rnpModel, context, httpRequest, httpServletResponse);
//        return backgroundResponse;
//    }

    private BackgroundResponse setInBackground(RnPModel rnpModel) throws Exception {
        BackgroundResponse backgroundResponse;

        Context context = rnpModel.getContext();
        HttpServletRequest httpRequest = rnpModel.getHttpRequest();

        HttpServletResponse httpServletResponse = rnpModel.getHttpResponse();
        removeContextAndHttpRequestAndResponse(rnpModel);
        String mqUrl = PropertyReader.getProperty("rnp.multilevel.background.process.message.queue.url");
        String serializedModel = new JSON().serialize(rnpModel);
        backgroundResponse = syncServiceCall(mqUrl, serializedModel);
        setContextAndHttpRequestAndResponse(rnpModel, context, httpRequest, httpServletResponse);

        return backgroundResponse;
    }

    private BackgroundResponse setInBackground(ReportParameterModel reportParameter) throws Exception {
        BackgroundResponse backgroundResponse;

        Context context = reportParameter.getContext();
        HttpServletRequest httpRequest = reportParameter.getHttpRequest();
        HttpServletResponse httpServletResponse = reportParameter.getHttpResponse();

        removeContextAndHttpRequestAndResponse(reportParameter);

        String mqUrl = PropertyReader.getProperty("rnp.singlelevel.background.process.message.queue.url");
        String serializedModel = new JSON().serialize(reportParameter);
        backgroundResponse = syncServiceCall(mqUrl, serializedModel);
        setContextAndHttpRequestAndResponse(reportParameter, context, httpRequest, httpServletResponse);

        return backgroundResponse;
    }

    private void removeContextAndHttpRequestAndResponse(RnPModel rnpModel) {
        rnpModel.setContext(null);
        rnpModel.setHttpRequest(null);
        rnpModel.setHttpResponse(null);
    }

    private void setContextAndHttpRequestAndResponse(RnPModel rnpModel, Context context, HttpServletRequest httpRequest, HttpServletResponse httpServletResponse) {
        rnpModel.setContext(context);
        rnpModel.setHttpRequest(httpRequest);
        rnpModel.setHttpResponse(httpServletResponse);
    }

    @ResponseBody
    @PostMapping(value = "/multiLevelBomDataReport/backgroundProcess")
    public ResponseEntity backgroundProcessorService(HttpServletRequest httpRequest, HttpServletResponse httpServletResponse, @RequestBody final RnPModel rnpModel) throws Exception {
        RnPResponseHandler responseHandler = new RnPResponseHandler();

        Context context = null;
        try {
            context = getContext();
            setContextAndHttpRequestAndResponse(rnpModel, context, httpRequest, httpServletResponse);
            RnPBackgroundProcessImpl rnpBackgroundProcessImpl = new RnPBackgroundProcessImpl(rnpModel);
//            BackgroundProcessor<File> backgroundProcessor = new BackgroundProcessor<>();

            BackgroundRunnable<File> backgroundRunnable = new BackgroundRunnable(rnpBackgroundProcessImpl);
            backgroundRunnable.backGroundProcess();
            return responseHandler.getBackgroundProcessResponse();

        } catch (Exception exp) {
            EXPORT_BOM_LOGGER.error("Report generation error: " + exp.getMessage());
            return responseHandler.getErrorResponse(exp);
        } finally {
            if (context != null) {
                context.close();
                context = null;
            }
        }
    }

    private Context getContext() {
        Context context = null;
        try {
            CreateContext createContext = new CreateContext();

            context = createContext.getAdminContext();
            if (!context.isConnected()) {
                throw new Exception(Constants.CONTEXT_EXCEPTION);
            }
        } catch (Exception exp) {
            EXPORT_BOM_LOGGER.error("Report generation error: " + exp.getMessage());
            throw new RuntimeException(exp);
        }
        return context;
    }

    public BackgroundResponse asyncServiceCall(String uri, String data) throws Exception {
        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .POST(BodyPublishers.ofString(data))
                .build();

        client.sendAsync(request, BodyHandlers.discarding());

        BackgroundResponse backgroundResponse = new BackgroundResponse<>();
        backgroundResponse.setResponse("true");
        return backgroundResponse;
    }

    public BackgroundResponse syncServiceCall(String uri, String data) throws Exception {
        BackgroundResponse backgroundResponse = new BackgroundResponse<>();
        try {
            ServiceRequester prepareRequest = new ServiceRequester();
            String callService = prepareRequest.callService(uri, data);

            prepareResponseFromMQResponse(callService, backgroundResponse);

        } catch (Exception exp) {
            EXPORT_BOM_LOGGER.error(exp);
            throw exp;
        }
        return backgroundResponse;
    }

    private void prepareResponseFromMQResponse(String callService, BackgroundResponse backgroundResponse) throws RuntimeException {
        JSON json = new JSON();
        CommonResponse response = json.deserialize(callService, CommonResponse.class);
        if (response.getStatus().toString().equalsIgnoreCase(Status.OK.toString())) {
            backgroundResponse.setResponse("true");
            backgroundResponse.setResponse(json.serialize(response.getData()));
        } else {
            if (response.getMessages().size() > 0) {
                List<Object> messages = response.getMessages();
                throw new RuntimeException(json.serialize(messages));
            } else {
                List<String> systemErrors = response.getSystemErrors();
                throw new RuntimeException(systemErrors.toString());
            }
        }
    }

    private Boolean isBackgroundProcess(RnPModel rnpModel) throws FrameworkException, NumberFormatException {
        try {
            Long numberOfChildren = getStructureCount(rnpModel);
            EXPORT_BOM_LOGGER.info("RnP number of child in structure : " + numberOfChildren);
            rnpModel.setNumberOfChildInTheStructure(numberOfChildren);
            Boolean isBackgroundProcess = numberOfChildren > Long.parseLong(PropertyReader.getProperty("rnp.background.process.large.structure.max.items.count"));
            return isBackgroundProcess;
        } catch (Exception exp) {
            EXPORT_BOM_LOGGER.error(exp.getMessage());
            throw exp;
        }
    }

    private Long getStructureCount(RnPModel rnpModel) throws NumberFormatException, FrameworkException {
        try {
            //String mapsAbsoluteDirectory = PropertyReader.getProperty("bom.export.type.map.directory." + rnpModel.getType());
            if (NullOrEmptyChecker.isNullOrEmpty(DIRECTORY_MAP)) {
                DIRECTORY_MAP = PropertyReader.getProperties("reporting.printing.map.directory", true);
            }
            String mapsAbsoluteDirectory = DIRECTORY_MAP.get(rnpModel.getType());

            ObjectTypesAndRelations objectTypesAndRelations = new ObjectTypesAndRelations(mapsAbsoluteDirectory);
            List<String> relationshipNames = objectTypesAndRelations.getRelationshipNames();
            StringJoiner relJoiner = new StringJoiner(",");
            relationshipNames.forEach(relJoiner::add);
            List<String> typeNames = objectTypesAndRelations.getTypeNames();
            StringJoiner typeJoiner = new StringJoiner(",");
            typeNames.forEach(typeJoiner::add);

            String rnpCountQuery;
            if (!NullOrEmptyChecker.isNullOrEmpty(rnpModel.getObjectId())) {
                rnpCountQuery = "eval expression 'count TRUE' on expand bus '" + rnpModel.getObjectId() + "' from rel '" + relJoiner.toString() + "' type '" + typeJoiner.toString() + "' recurse to all";
            } else {
                rnpCountQuery = "eval expression 'count TRUE' on expand bus '" + rnpModel.getType() + "' '" + rnpModel.getName() + "' '" + rnpModel.getRev() + "' from rel '" + relJoiner.toString() + "' type '" + typeJoiner.toString() + "' recurse to all";
            }

            EXPORT_BOM_LOGGER.info("RnP number of child in structure count query : " + rnpCountQuery);
            Long numberOfChildren = Long.parseLong(MqlUtil.mqlCommand(rnpModel.getContext(), rnpCountQuery));
            return numberOfChildren;
        } catch (FrameworkException exp) {
            EXPORT_BOM_LOGGER.error(exp.getMessage());
            throw exp;
        }
    }

    @ResponseBody
    @GetMapping(value = "/multiLevelBomExport", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PDF_VALUE})
    public Object generateBomReport(HttpServletRequest httpRequest, HttpServletResponse response,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "name", required = false) String name, // code level validation for this parameter
            @RequestParam(value = "rev", required = false) String rev,
            @RequestParam(value = "isLatest", required = false, defaultValue = "false") boolean latest,
            @RequestParam(value = "objectId", required = false) String objectId,
            @RequestParam(value = "format", required = false, defaultValue = "json") String format,
            @RequestParam(value = "lang", required = false, defaultValue = "En") String lang,
            @RequestParam(value = "primaryLang", required = false) String primaryLang,
            @RequestParam(value = "secondaryLang", required = false) String secondaryLang,
            @RequestParam(value = "requestId", required = false) String requestId,
            @RequestParam(value = "expandLevel", required = false) String expandLevel,
            @RequestParam(value = "requester", required = false) String requester,
            @RequestParam(value = "printDrawing", required = false) String isDrawingInfoRequired,
            @RequestParam(value = "attrs", required = false) String attributeString,
            @RequestParam(value = "download", required = false, defaultValue = "true") boolean download,
            @RequestParam(value = "mainProjTitle", required = false) String mainProjTitle,
            @RequestParam(value = "psk", required = false) String psk,
            @RequestParam(value = "subTitle", required = false) String subTitle,
            @RequestParam(value = "product", required = false) String product,
            @RequestParam(value = "printDelivery", required = false) String printDelivery,
            @RequestParam(value = "treeView", required = false) String treeView,
            @RequestParam(value = "drawingType", required = false) String docType
    ) {
        long startTime = System.currentTimeMillis();
        if (!NullOrEmptyChecker.isNullOrEmpty(name) && name.contains("-")) {
            String[] part = name.split("-", 2);
            String lowarCase = part[0].toLowerCase();
            name = lowarCase + "-" + part[1];
        }
        EXPORT_BOM_LOGGER.info("Url parameters(For Report): Type: " + type + " ,Name: " + name
                + " ,Format: " + format + " ,Lang: " + lang + " ,Download: " + download + " ,RequestId: " + requestId + ", Expand Level: " + expandLevel);

        ResponseEntity responseEntity;
        String responseBomData = "";
        Context context = null;
        IResponse responseBuilder = new CustomResponseBuilder();
        boolean isMBOMReport = false;
        if (type.equalsIgnoreCase(Constant.DEFAULT_TYPE) || type.equals("CreateAssembly") || type.equals("ProcessContinuousCreateMaterial")) {
            isMBOMReport = true;
        }
        String buildResponse;
        try {
            String user = httpRequest.getHeader("user");
            String pass = httpRequest.getHeader("pass");
            String host = PropertyReader.getProperty("matrix.context.cas.connection.host");
            if (NullOrEmptyChecker.isNullOrEmpty(user) || NullOrEmptyChecker.isNullOrEmpty(pass)) {
                ContextPasswordSecurity contextPasswordSecurity = new ContextPasswordSecurity();
                user = contextPasswordSecurity.decryptPassword(PropertyReader.getProperty("context.name"));
                pass = contextPasswordSecurity.decryptPassword(PropertyReader.getProperty("context.pass"));
                // return new ResponseEntity<>(Constants.AUTHENTICATION_EXCEPTION, HttpStatus.NOT_ACCEPTABLE);
            }
            try {
                CreateContext createContext = new CreateContext();

                context = createContext.createCasContext(user, pass, host);
                if (!context.isConnected()) {
                    throw new Exception(Constants.CONTEXT_EXCEPTION);
                }
            } catch (Exception exp) {
                return new ResponseEntity<>(Constants.CONTEXT_EXCEPTION, HttpStatus.NOT_ACCEPTABLE);
            }
            MqlQueries mqlQuery = new MqlQueries();
            objectId = mqlQuery.getObjectId(context, type, name, rev);
            if (NullOrEmptyChecker.isNullOrEmpty(objectId)) {
                buildResponse = responseBuilder.setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.FAILED).addErrorMessage(Constants.TYPE_NAME_REVISION_BE_NULL_EXCEPTION).setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.FAILED).buildResponse();
                EXPORT_BOM_LOGGER.error(buildResponse);
                return new ResponseEntity<>(buildResponse, HttpStatus.NOT_ACCEPTABLE);
            }

            if (latest) {
                BomExportUtil bomExportUtil = new BomExportUtil();
                rev = bomExportUtil.getLatestRevision(context, type, name);
            }
            if (NullOrEmptyChecker.isNullOrEmpty(rev)) {
                buildResponse = responseBuilder.setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.FAILED).addErrorMessage(Constants.LATEST_REVISION_BE_NULL_EXCEPTION).setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.FAILED).buildResponse();
                EXPORT_BOM_LOGGER.error(buildResponse);
                return new ResponseEntity<>(buildResponse, HttpStatus.NOT_ACCEPTABLE);
            }
            if (format.equals(Constants.PDF) || format.equals(Constants.XLS)) {
                RnPBomData rnpBomData = new RnPBomData();
                responseEntity = rnpBomData.generateBomData(httpRequest, response, type, name, rev, objectId, expandLevel, isDrawingInfoRequired, "", attributeString, printDelivery, mainProjTitle, psk, subTitle, product, lang, primaryLang, secondaryLang, format, requestId, context, docType);

                responseBomData = (String) responseEntity.getBody();
            } else {
                BomExportUtil bomExportUtil = new BomExportUtil();
                HashMap<String, String> urlParams = bomExportUtil.getUrlParamValues(type, name, rev, objectId, expandLevel, format, lang, primaryLang, secondaryLang, requestId, requester, treeView);
                responseEntity = bomExportUtil.generateBomExportData(httpRequest, response, urlParams, attributeString, context, docType);
                if (!responseEntity.getStatusCode().equals(HttpStatus.OK)) {
                    return responseEntity;
                }
                responseBomData = (String) responseEntity.getBody();
                if (Boolean.parseBoolean(urlParams.get("treeView"))) {
                    MultiLevelBOMStructureUtils treeStructureUtils = new MultiLevelBOMStructureUtils(responseBomData);
                    responseBomData = treeStructureUtils.getTreeStructuredJson().toString();
                }
                EXPORT_BOM_LOGGER.debug("Response BOM Data: " + responseBomData);
                if (format.equalsIgnoreCase(Constants.JSON)) {
                    response.setHeader("Content-Type", "application/json; charset=UTF-8");
                    httpRequest.setAttribute("Content-Type", "application/json; charset=UTF-8");
                    httpRequest.setAttribute("Accept", "application/json; charset=UTF-8");
                    return new ResponseEntity<>(responseBomData, HttpStatus.OK);
                }
            }
            if (!responseBomData.isEmpty()) {
                HashMap rootItemParams = CommonUtil.getInfoMapFromBomDataResponse(responseBomData, "rootItemInfo");
                EXPORT_BOM_LOGGER.debug("Response of Root param: " + rootItemParams);
                HashMap deliveryParams = null;
                if (!NullOrEmptyChecker.isNullOrEmpty(printDelivery) && Boolean.parseBoolean(printDelivery)) {
                    deliveryParams = CommonUtil.getInfoMapFromBomDataResponse(responseBomData, "Delivery Project Info");
                }
                JasperReportGenerator jasperReportGenerator = new JasperReportGenerator();
                try {
                    Map<String, String> responseData = jasperReportGenerator.generateReport(responseBomData, rootItemParams, deliveryParams, format, lang, requestId, false, type, isMBOMReport);
                    String outputFile = responseData.get("filePath");
                    if (!outputFile.isEmpty()) {
                        if (download) {
                            return generateBOMReport(outputFile, response);
                        }
                    } else {
                        EXPORT_BOM_LOGGER.debug("Failed to generate report file.");
                    }
                } catch (Exception exp) {
                    EXPORT_BOM_LOGGER.debug(exp.getMessage());
                } finally {
                    jasperReportGenerator = null;
                }
            } else {
                EXPORT_BOM_LOGGER.info("Response data is empty.");
            }
        } catch (Exception e) {
            EXPORT_BOM_LOGGER.debug(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_ACCEPTABLE);
        } finally {
            if (context != null) {
                context.close();
                context = null;
            }
        }

        return new ResponseEntity<>(Constants.REPORT_GENERATION_MESSAGE, HttpStatus.OK);
    }

    @ResponseBody
    @PostMapping(value = "/singleLevelBomDataReport/backgroundProcess")
    public ResponseEntity singleLevelBackgroundProcessorService(HttpServletRequest httpRequest, HttpServletResponse httpServletResponse, @RequestBody final ReportParameterModel reportParameter) throws Exception {
        RnPResponseHandler responseHandler = new RnPResponseHandler();
        try {
            Context context = getContext();

            setContextAndHttpRequestAndResponse(reportParameter, context, httpRequest, httpServletResponse);

            RnPBackgroundProcessSingleLevelImpl rnpBackgroundProcessImpl = new RnPBackgroundProcessSingleLevelImpl(reportParameter);
//            BackgroundProcessor<File> backgroundProcessor = new BackgroundProcessor<>();

            BackgroundRunnable<File> backgroundRunnable = new BackgroundRunnable(rnpBackgroundProcessImpl);
            backgroundRunnable.backGroundProcess();
            return responseHandler.getBackgroundProcessResponse();

        } catch (Exception exp) {
            EXPORT_BOM_LOGGER.error("Report generation error: " + exp.getMessage());
            return responseHandler.getErrorResponse(exp);
        }
    }

    @ResponseBody
    @GetMapping(path = "/singleLevelBomDataReport")
    public Object generateSingleLevelBOMReport(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "rev", required = false) String rev,
            @RequestParam(value = "objectId", required = false) String objectId,
            @RequestParam(value = "expandLevel", required = false) String expandLevel,
            @RequestParam(value = "attrs", required = false) String attributeString,
            @RequestParam(value = "format", required = false) String format,
            @RequestParam(value = "lang", required = false) String lang,
            @RequestParam(value = "primaryLang", required = false) String primaryLang,
            @RequestParam(value = "secondaryLang", required = false) String secondaryLang,
            @RequestParam(value = "requestId", required = false) String requestId,
            @RequestParam(value = "mainProjTitle", required = false) String mainProjTitle,
            @RequestParam(value = "psk", required = false) String psk,
            @RequestParam(value = "subTitle", required = false) String subTitle,
            @RequestParam(value = "product", required = false) String product,
            @RequestParam(value = "docType", required = false) String docType,
            @RequestParam(value = "printDrawing", required = false) String isDrawingInfoRequired,
            @RequestParam(value = "printSummary", required = false) String isSummaryRequired,
            @RequestParam(value = "printDelivery", required = false) String printDelivery,
            @RequestParam(value = "download", required = false) boolean download,
            @RequestParam(value = "receiverEmail", required = false) String receiverEmail) {

        Date controllerStartTime = DateTimeUtils.getTime(new Date());
        Instant singleLevelReportStartTime = Instant.now();
        EXPORT_BOM_LOGGER.debug("---------------------- ||| SINGLE LEVEL REPORT CONTROLLER BEGIN ||| ----------------------");
        EXPORT_BOM_LOGGER.debug("##########################################################################################");

        IResponse responseBuilder = new CustomResponseBuilder();
        String buildResponse;
        RnPResponseHandler responseHandler = new RnPResponseHandler();

        try {
            Context context = ContextProvider.provideContext();
            ReportParameterModel reportParameter = new ReportParameterModel();
            reportParameter.setContext(context);
            reportParameter.setType(type);
            reportParameter.setName(name);
            reportParameter.setRev(rev);
            reportParameter.setObjectId(objectId);
            reportParameter.setExpandLevel(expandLevel);
            reportParameter.setAttributeListString(attributeString);
            reportParameter.setFormat(format);
            reportParameter.setLang(lang);
            reportParameter.setPrimaryLang(primaryLang);
            reportParameter.setSecondaryLang(secondaryLang);
            reportParameter.setRequestId(requestId);
            reportParameter.setMainProjTitle(mainProjTitle);
            reportParameter.setPsk(psk);
            reportParameter.setSubTitle(subTitle);
            reportParameter.setProduct(product);
            reportParameter.setDocType(docType);
            reportParameter.setIsDrawingInfoRequired(isDrawingInfoRequired);
            reportParameter.setIsSummaryRequired(isSummaryRequired);
            reportParameter.setPrintDelivery(printDelivery);
            reportParameter.setDownload(download);
            reportParameter.setHttpRequest(httpRequest);
            reportParameter.setHttpResponse(httpResponse);
            reportParameter.setReceiverEmail(receiverEmail);
            reportParameter.setBaseUrl(ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString());

            boolean isMBOMReport = false;
            if (type.equalsIgnoreCase(Constant.DEFAULT_TYPE) || type.equals("CreateAssembly") || type.equals("ProcessContinuousCreateMaterial")) {
                isMBOMReport = true;
            }

            reportParameter.setIsMBOMReport(isMBOMReport);

            Boolean isBackgroundProcess = isBackgroundProcess(reportParameter);

            if (isBackgroundProcess && NullOrEmptyChecker.isNullOrEmpty(receiverEmail)) {
                throw new NullPointerException(type + " " + name + " " + rev + " is a large structure. Need report receiver mail address");
            }

            BackgroundResponse backgroundResponse = null;

            if (isBackgroundProcess) {
                backgroundResponse = setInBackground(reportParameter);
            } else {
                RnPBackgroundProcessSingleLevelImpl rnpBackgroundProcessImpl = new RnPBackgroundProcessSingleLevelImpl(reportParameter);
                BackgroundProcessor<File> backgroundProcessor = new BackgroundProcessor<>();
                backgroundResponse = backgroundProcessor.backGroundProcess(rnpBackgroundProcessImpl, isBackgroundProcess);
            }

            return responseHandler.getResponse(isBackgroundProcess, reportParameter);

        } catch (Exception e) {
            buildResponse = responseBuilder.addErrorMessage(e.getMessage()).setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.FAILED).buildResponse();
            EXPORT_BOM_LOGGER.error(buildResponse);
            return new ResponseEntity<>(buildResponse, HttpStatus.NOT_ACCEPTABLE);
        } finally {
            Date controllerEndTime = DateTimeUtils.getTime(new Date());
            EXPORT_BOM_LOGGER.debug("Time elapsed for 'singleLevelBomDataReport' service is : " + DateTimeUtils.elapsedTime(controllerStartTime, controllerEndTime, null, null));

            Instant singleLevelReportEndTime = Instant.now();
            long duration = DateTimeUtils.getDuration(singleLevelReportStartTime, singleLevelReportEndTime);

            EXPORT_BOM_LOGGER.debug("Single Level Report Generation Process has taken : '" + duration + "' milli-seconds");
            EXPORT_BOM_LOGGER.debug("---------------------------------------- ||| SINGLE LEVEL REPORT CONTROLLER END ||| ----------------------------------------");
            EXPORT_BOM_LOGGER.debug("###########################################################################################################################\n");
        }
    }

    private byte[] generateBOMReport(String outputFile, HttpServletResponse httpServletResponse) throws FileNotFoundException, IOException {
        Path path = Paths.get(outputFile);
        String applicationType;
        String probeContentType = Files.probeContentType(path);

        if (probeContentType == null || probeContentType.equalsIgnoreCase("")) {
            applicationType = "application/octet-stream";
        } else {
            applicationType = probeContentType;
        }
        httpServletResponse.setContentType(applicationType);
        File reportFile = path.toFile();
        httpServletResponse.setHeader("Content-Disposition", "attachment; filename=\"" + reportFile.getName() + "\"");
        InputStream inputStream = new FileInputStream(reportFile);

        return IOUtils.toByteArray(inputStream);
    }

    /**
     * Service to fetch attribute labels for R&P widget to populate within the
     * attribute selection field.
     */
    @ResponseBody
    @GetMapping(value = "/allSelectableAttributes", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity getAllSelectableAttributes(HttpServletRequest httpRequest, HttpServletResponse response,
            @RequestParam(value = "type", required = false) String type, @RequestParam(value = "requester", required = false) String requester) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException, Exception {

        SelectedAtrributeFetch selectedAtrribute = new SelectedAtrributeFetch();
        ResponseEntity allSelectableAttributes = selectedAtrribute.getAllSelectableAttributes(httpRequest, response, type, requester);
        return allSelectableAttributes;
    }

//    @ResponseBody
//
//    @GetMapping(value = "/allSelectableAttributesForBom", produces = {MediaType.APPLICATION_JSON_VALUE})
//    public ResponseEntity getAllSelectableAttributesForBom(HttpServletRequest httpRequest, HttpServletResponse response,
//            @RequestParam(value = "type", required = false) String type, @RequestParam(value = "requester", required = false) String requester) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException, Exception {
//
//        BomExportUtil bomExportUtil = new BomExportUtil();
//        ResponseEntity allSelectableAttributes = bomExportUtil.getAllSelectableAttributes(httpRequest, response, type, requester);
//
//        return allSelectableAttributes;
//    }

    @ResponseBody
    @GetMapping(value = "/v1/ln/items/nightly/dw-enovia", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Object> dwToEnoviaItemUpdateNightly() {
        Instant startTime = Instant.now();
        IResponse responseBuilder = new CustomResponseBuilder();
        String buildResponse = "";
        try {
            EnoviaNightlyUpdateAction action = new EnoviaNightlyUpdateAction();
            buildResponse = responseBuilder
                    .setData("Successfully transferred dw item update to ENOVIA")
                    .setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.OK)
                    .buildResponse();
            return new ResponseEntity<>(buildResponse, HttpStatus.OK);
        } catch (Exception e) {
            EXPORT_BOM_LOGGER.error("Error: " + e.getMessage());
            buildResponse = responseBuilder
                    .addErrorMessage("Error: " + e.getMessage())
                    .setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.FAILED)
                    .buildResponse();
            return new ResponseEntity<>(buildResponse, HttpStatus.NOT_ACCEPTABLE);
        } finally {
            Instant endTime = Instant.now();
            Duration timeTaken = Duration.between(startTime, endTime);
            EXPORT_BOM_LOGGER.info("Time taken for DW-Enovia Item Update Service : " + timeTaken.toMillis());
        }
    }

//    @ResponseBody
//    @PostMapping(value = "/v1/ln/transfer/listedBom", produces = {MediaType.APPLICATION_JSON_VALUE}, consumes = {MediaType.APPLICATION_JSON_VALUE})
//    public ResponseEntity<Object> listedItemAndBOMTransfer(@RequestBody LNTransferRequestModel itemTransferModel) throws Exception {
//        LNTransferAction lNTransferAction = new LNTransferAction();
//        IResponse responseBuilder = new CustomResponseBuilder();
//        LNTransferService lnTransferService = new LNTransferServiceImpl();
//        String buildResponse = "";
//        try {
//            LNRequestUtil.validateRequest(itemTransferModel);
//            Map<String, List<ResponseMessageFormaterBean>> transferResultMap = lnTransferService.itemAndBOMTransfer(itemTransferModel);
//            List<ResponseMessageFormaterBean> successfulItemList = transferResultMap.get(LNTransferServiceImpl.SUCCESSFUL_ITEM_LIST);
//            List<ResponseMessageFormaterBean> failedItemList = transferResultMap.get(LNTransferServiceImpl.FAILED_ITEM_LIST);
//            if (successfulItemList.size() > 0 && failedItemList.size() > 0) {
//                buildResponse = responseBuilder.setData(successfulItemList)
//                        .setData(successfulItemList)
//                        .addErrorMessage(failedItemList)
//                        .setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.FAILED)
//                        .buildResponse();
//                return new ResponseEntity<>(buildResponse, HttpStatus.OK);
//            } else {
//                if (failedItemList.size() > 0) {
//                    buildResponse = responseBuilder.addErrorMessage(failedItemList)
//                            .setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.FAILED)
//                            .buildResponse();
//                    return new ResponseEntity<>(buildResponse, HttpStatus.OK);
//                } else if (successfulItemList.size() > 0) {
//                    buildResponse = responseBuilder.setData(successfulItemList)
//                            .setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.OK)
//                            .buildResponse();
//                    return new ResponseEntity<>(buildResponse, HttpStatus.OK);
//                }
//            }
//            buildResponse = responseBuilder.setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.OK).buildResponse();
//            return new ResponseEntity<>(buildResponse, HttpStatus.OK);
//        } catch (IllegalArgumentException ex) {
//            EXPORT_BOM_LOGGER.error(ex);
//            buildResponse = responseBuilder.addErrorMessage(ex.getMessage()).setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.FAILED).buildResponse();
//            return new ResponseEntity<>(buildResponse, HttpStatus.BAD_REQUEST);
//        } catch (ConnectException ex) {
//            EXPORT_BOM_LOGGER.error(ex);
//            buildResponse = responseBuilder.addErrorMessage(ex.getMessage()).setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.FAILED).buildResponse();
//            return new ResponseEntity<>(buildResponse, HttpStatus.INTERNAL_SERVER_ERROR);
//        } catch (Exception ex) {
//            EXPORT_BOM_LOGGER.error(ex);
//            buildResponse = responseBuilder.addErrorMessage(ex.getMessage()).setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.FAILED).buildResponse();
//            return new ResponseEntity<>(buildResponse, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
//
//    @ResponseBody
//    @PostMapping(value = "/v1/ln/transfer/item", produces = {MediaType.APPLICATION_JSON_VALUE}, consumes = {MediaType.APPLICATION_JSON_VALUE})
//    public ResponseEntity<Object> itemTransfer(@RequestBody LNTransferRequestModel itemTransferModel, @RequestParam(value = "level", required = false) String level) {
//        LNTransferAction lNTransferAction = new LNTransferAction();
//        IResponse responseBuilder = new CustomResponseBuilder();
//        LNTransferService lnTransferService = new LNTransferServiceImpl();
//        String expandLevel = "99";
//        if (!NullOrEmptyChecker.isNullOrEmpty(level)) {
//
//            expandLevel = level;
//        }
//        String buildResponse = "";
//        try {
//            LNRequestUtil.validateRequest(itemTransferModel);
//            Map<String, List<ResponseMessageFormaterBean>> transferResultMap = lnTransferService.itemTransfer(itemTransferModel, expandLevel);
//            List<ResponseMessageFormaterBean> successfulItemList = transferResultMap.get(LNTransferServiceImpl.SUCCESSFUL_ITEM_LIST);
//            List<ResponseMessageFormaterBean> failedItemList = transferResultMap.get(LNTransferServiceImpl.FAILED_ITEM_LIST);
//            if (successfulItemList.size() > 0 && failedItemList.size() > 0) {
//                buildResponse = responseBuilder.setData(successfulItemList)
//                        .setData(successfulItemList)
//                        .addErrorMessage(failedItemList)
//                        .setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.FAILED)
//                        .buildResponse();
//                return new ResponseEntity<>(buildResponse, HttpStatus.OK);
//            } else {
//                if (failedItemList.size() > 0) {
//                    buildResponse = responseBuilder.addErrorMessage(failedItemList)
//                            .setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.FAILED)
//                            .buildResponse();
//                    return new ResponseEntity<>(buildResponse, HttpStatus.OK);
//                } else if (successfulItemList.size() > 0) {
//                    buildResponse = responseBuilder.setData(successfulItemList)
//                            .setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.OK)
//                            .buildResponse();
//                    return new ResponseEntity<>(buildResponse, HttpStatus.OK);
//                }
//            }
//            buildResponse = responseBuilder.setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.OK).buildResponse();
//            return new ResponseEntity<>(buildResponse, HttpStatus.OK);
//        } catch (IllegalArgumentException ex) {
//            EXPORT_BOM_LOGGER.error(ex);
//            buildResponse = responseBuilder.addErrorMessage(ex.getMessage()).setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.FAILED).buildResponse();
//            return new ResponseEntity<>(buildResponse, HttpStatus.BAD_REQUEST);
//        } catch (ConnectException ex) {
//            EXPORT_BOM_LOGGER.error(ex);
//            buildResponse = responseBuilder.addErrorMessage(ex.getMessage()).setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.FAILED).buildResponse();
//            return new ResponseEntity<>(buildResponse, HttpStatus.INTERNAL_SERVER_ERROR);
//        } catch (Exception ex) {
//            EXPORT_BOM_LOGGER.error(ex);
//            buildResponse = responseBuilder.addErrorMessage(ex.getMessage()).setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.FAILED).buildResponse();
//            return new ResponseEntity<>(buildResponse, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
//
//    @ResponseBody
//    @PostMapping(value = "/v1/ln/transfer/listedItem", produces = {MediaType.APPLICATION_JSON_VALUE}, consumes = {MediaType.APPLICATION_JSON_VALUE})
//    public ResponseEntity<Object> listedItemTransfer(@RequestBody LNTransferRequestModel itemTransferModel) {
//        LNTransferAction lNTransferAction = new LNTransferAction();
//        IResponse responseBuilder = new CustomResponseBuilder();
//        LNTransferService lnTransferService = new LNTransferServiceImpl();
//        String buildResponse = "";
//        try {
//            LNRequestUtil.validateRequest(itemTransferModel);
//            Map<String, List<ResponseMessageFormaterBean>> transferResultMap = lnTransferService.itemTransfer(itemTransferModel);
//            List<ResponseMessageFormaterBean> successfulItemList = transferResultMap.get(LNTransferServiceImpl.SUCCESSFUL_ITEM_LIST);
//            List<ResponseMessageFormaterBean> failedItemList = transferResultMap.get(LNTransferServiceImpl.FAILED_ITEM_LIST);
//            if (successfulItemList.size() > 0 && failedItemList.size() > 0) {
//                buildResponse = responseBuilder.setData(successfulItemList)
//                        .setData(successfulItemList)
//                        .addErrorMessage(failedItemList)
//                        .setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.FAILED)
//                        .buildResponse();
//                return new ResponseEntity<>(buildResponse, HttpStatus.OK);
//            } else {
//                if (failedItemList.size() > 0) {
//                    buildResponse = responseBuilder.addErrorMessage(failedItemList)
//                            .setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.FAILED)
//                            .buildResponse();
//                    return new ResponseEntity<>(buildResponse, HttpStatus.OK);
//                } else if (successfulItemList.size() > 0) {
//                    buildResponse = responseBuilder.setData(successfulItemList)
//                            .setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.OK)
//                            .buildResponse();
//                    return new ResponseEntity<>(buildResponse, HttpStatus.OK);
//                }
//            }
//            buildResponse = responseBuilder.setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.OK).buildResponse();
//            return new ResponseEntity<>(buildResponse, HttpStatus.OK);
//        } catch (IllegalArgumentException ex) {
//            EXPORT_BOM_LOGGER.error(ex);
//            buildResponse = responseBuilder.addErrorMessage(ex.getMessage()).setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.FAILED).buildResponse();
//            return new ResponseEntity<>(buildResponse, HttpStatus.BAD_REQUEST);
//        } catch (ConnectException ex) {
//            EXPORT_BOM_LOGGER.error(ex);
//            buildResponse = responseBuilder.addErrorMessage(ex.getMessage()).setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.FAILED).buildResponse();
//            return new ResponseEntity<>(buildResponse, HttpStatus.INTERNAL_SERVER_ERROR);
//        } catch (Exception ex) {
//            EXPORT_BOM_LOGGER.error(ex);
//            buildResponse = responseBuilder.addErrorMessage(ex.getMessage()).setStatus(com.bjit.common.rest.app.service.payload.common_response.Status.FAILED).buildResponse();
//            return new ResponseEntity<>(buildResponse, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
    private void clearObjects(BusinessObject businessObject, Context context, IResponse responseBuilder) throws MatrixException {
        if (!NullOrEmptyChecker.isNull(businessObject)) {
            businessObject.close(context);
            businessObject = null;
        }
        if (context != null) {
            context.close();
            context = null;
        }
        responseBuilder = null;
    }

    private HttpHeaders addTokenToResponse() {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("token", this.token);
        return responseHeaders;
    }

    private void getUserCredentials(HttpServletRequest httpRequest) throws Exception {
        try {
            VerifyToken verifyToken = new VerifyToken();
            HashMap<String, Object> credentialsMap = verifyToken.getCredentialsMap(httpRequest);
            this.token = credentialsMap.get("token").toString();
            ObjectMapper<Object, AuthenticationUserModel> objectMapper = new ObjectMapper();
            objectMapper.setObjects(credentialsMap.get("credentials"), AuthenticationUserModel.class);
            this.userCredentialsModel = objectMapper.getObject();
        } catch (Exception exp) {
            throw exp;
        }
    }
}
