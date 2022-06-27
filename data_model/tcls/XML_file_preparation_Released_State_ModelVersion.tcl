#############################################################
# @Name: XML file preparaion on released state in Model Version type
# @Version: 1.0
# @Created By: BJIT Ltd.
# @Date: 20-04-2022
#############################################################
tcl;
eval {
	set fp_LogFile [open "XML_file_preparaion_on_released_for_ModelVersion_type.log" "a"]
	proc writeLog { fp_LogFile message } {
		set currenttime [clock format [clock seconds] -format "%m/%d/20%y %A %I:%M:%S %p"]
		set outputmessage [ append "Time : " $currenttime " : " $message]
		puts $fp_LogFile $outputmessage
		flush $fp_LogFile
	}

	set deleteExistingTriggerObj [ catch {

        puts "Going to find existing Released item Trigger object !!!"
		writeLog $fp_LogFile "Going to find existing Released item Trigger object !!!"
	
		set triggerType [ split [ mql temp query bus "eService Trigger Program Parameters" "ProductStateChangeTrigger" * select id dump ] , ]
		set triggerObjId [ lindex $triggerType 3 ]
	
		puts "Trigger object id : $triggerObjId"
		writeLog $fp_LogFile "Trigger object id : $triggerObjId"
	
		puts "Going to delete trigger !!!"
		writeLog $fp_LogFile "Going to delete trigger !!!"
	
		mql delete bus $triggerObjId
	
		puts "Delete trigger done !!!"
		writeLog $fp_LogFile "Delete trigger done !!!"

	} errorMsg ]

	if { $deleteExistingTriggerObj == 0 } {
            puts "Existing trigger deleted successfully"
            writeLog $fp_LogFile "Existing trigger deleted successfully !!!"
	} else {
            puts "No existing trigger found"
            writeLog $fp_LogFile "No existing trigger found !!!"
        }

	puts "Going to create Trigger object !!!"
	writeLog $fp_LogFile "Going to create Trigger object !!!"
	
	mql add bus "eService Trigger Program Parameters"  ProductStateChangeTrigger -\
	policy "eService Trigger Program Policy"\
	vault "eService Administration"\
	description "Generate XML file at Release state of ModelVersion type"\
	"eService Program Argument 1" "\${OBJECTID}"\
	"eService Program Argument 2" "\${STATENAME}"\
	"eService Program Argument 3" "\${NEXTSTATE}"\
	"eService Program Argument 4" "\${NAME}"\
	"eService Program Argument 5" "\${REVISION}"\
	"eService Program Argument 6" "\${TYPE}"\
	"eService Program Name" "emxProductCustomTriggerProcess"\
	"eService Constructor Arguments" "\${OBJECTID} \${STATENAME} \${NEXTSTATE} \${NAME} \${REVISION} \${TYPE}"\
	"eService Method Name" "CommonStateChangeFileGeneration"\
	"eService Sequence Number" 102
	
	puts "Trigger object creation done !!!"
	writeLog $fp_LogFile "Trigger object creation done !!!"
	
	puts "Add Triger to policy !!!"
	writeLog $fp_LogFile "Add Triger to policy !!!"
	
	mql modify policy "Product"\
    state Review remove trigger promote action\
    add trigger promote action emxTriggerManager  input "PolicyProductStateReviewPromoteAction ProductStateChangeTrigger"
	
	mql modify policy "Product"\
    state Release remove trigger promote action\
    add trigger promote action emxTriggerManager  input "PolicyProductStateReleasePromoteAction ProductStateChangeTrigger"
	
	mql modify policy "Product"\
    state 'Design Engineering' remove trigger promote action\
    add trigger promote action emxTriggerManager  input "ProductStateChangeTrigger"
	
	puts "Trigger addition done !!!"
	writeLog $fp_LogFile "Trigger addition done !!!"
	
	set triggerType [ split [ mql temp query bus "eService Trigger Program Parameters" "ProductStateChangeTrigger" * select id dump ] , ]
	set triggerObjId [ lindex $triggerType 3 ]
	
	puts "Trigger object id : $triggerObjId"
	writeLog $fp_LogFile "Trigger object id : $triggerObjId"
	
	puts "Going to modify trigger object state !!!"
	writeLog $fp_LogFile "Going to modify trigger object state !!!"
	
	mql modify bus $triggerObjId current Active
	
	puts "Modify trigger object state done !!!"
	writeLog $fp_LogFile "Modify trigger object state done !!!"
	
	
}