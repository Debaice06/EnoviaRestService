define("VALMET/PLMPDMBOMCompareUX/WidgetConfiguration", [], function() {
    var widgetConfiguration = {
        "BC_SEPARATE_INTEGRATION_SERVER_3DSPACE_URL": "https://dspp21space.plm.valmet.com:8180/3dspace",
        "BC_ENOVIA_REST_SERVICE_URL": "https://dspp21space.plm.valmet.com/EnoviaRestService",
        "BC_BOM_COMPARISON_SERVICE_URL": "/compareBOM/EnoviaPDMBOMComparison?",
        "BC_ALLOWED_TOP_ITEM_TYPES": "CreateAssembly,ProcessContinuousCreateMaterial,CreateMaterial",
        "BC_ALLOWED_PDM_ITEM_TYPES": "CreateAssembly,ProcessContinuousCreateMaterial",
        "BC_EXCLUDE_ITEM_TYPES_FROM_REVISION_COMPARE": "VAL_VALComponent,VAL_VALComponentMaterial",
        "BC_BOM_COMPARISON_SERVICE_ATTRIBUTES": "Type,name,revision,Qty,Position,id,DistributionList,HAG%20Drawing%20Number,physicalid,Short%20Name,Description,Unit,Material,Commodity%20Code,Commodity%20Code%20US,Commodity%20Code%20CH,Source%20Item,Weight,Material,Size,Status,Standard,ERP%20Item%20Type,Release%20purpose,Transfer%20To%20ERP,Width,Lenth,Drawing%20Number,Title,PDM%20revision,Technical%20Designation,Level,item%20common%20text,item%20purchasing%20text,bom%20common%20text,bom%20purchasing%20text,bom%20manufacturing%20text,Mastership",
        
        "BC_SELECTION_BOX_VISLIBITY_LIST": [{
            "displayName": "Revision",
            "name": "revision"
        }, {
            "displayName": "Quantity",
            "name": "qty"
        }, {
            "displayName": "Position",
            "name": "position"
        },  {
            "displayName": "Drawing Number",
            "name": "drawingNumber"
        },{
            "displayName": "Transfer To ERP",
            "name": "transferToERP"
        }],

        "BC_SELECTION_BOX_DEFAULT_LIST": ["revision","position", "qty", "drawingNumber","transferToERP"]
    };
    return widgetConfiguration;
});