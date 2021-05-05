update yang_resource set
name = 'cps-ran-schema-model2021-01-28.yang',
checksum = '436fef591eba7f38d1a0c5e3cbd3c122f01ab41dfab37cc5a9cbca1ed53b29fb',
content = 'module cps-ran-schema-model {
  yang-version 1.1;
  namespace "org:onap:ccsdk:features:sdnr:northbound:cps-ran-schema-model";
  prefix rn;

  import ietf-inet-types {
    prefix inet;
  }
  import ietf-yang-types {
    prefix yang;
  }

  organization
    "Open Network Automation Platform - ONAP
     <https://www.onap.org>";
  contact
    "Editors:
       Sandeep Shah
       <mailto:sandeep.shah@ibm.com>

       Swaminathan Seetharaman
       <mailto:swaminathan.seetharaman@wipro.com>";
  description
    "This module contains a collection of YANG definitions for capturing
     relationships among managed elements of the radio access Network
     to be stored in ONAP CPS platform.

     Copyright 2020-2021 IBM.

     Licensed under the Apache License, Version 2.0 (the ''''License'''');
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an ''''AS IS'''' BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.";

  revision 2021-01-28 {
    description
      "CPS RAN Network YANG Model for ONAP/O-RAN POC";
    reference
      "https://wiki.onap.org/display/DW/E2E+Network+Slicing+Use+Case+in+R7+Guilin";
  }

  typedef usageState {
    type enumeration {
      enum IDLE {
        description
          "TODO";
      }
      enum ACTIVE {
        description
          "TODO";
      }
      enum BUSY {
        description
          "TODO";
      }
    }
    description
      "It describes whether or not the resource is actively in
       use at a specific instant, and if so, whether or not it has spare
       capacity for additional users at that instant. The value is READ-ONLY.";
    reference
      "ITU T Recommendation X.731";
  }

  typedef Mcc {
    type string;
    description
      "The mobile country code consists of three decimal digits,
       The first digit of the mobile country code identifies the geographic
       region (the digits 1 and 8 are not used):";
    reference
      "3GPP TS 23.003 subclause 2.2 and 12.1";
  }

  typedef Mnc {
    type string;
    description
      "The mobile network code consists of two or three
       decimal digits (for example: MNC of 001 is not the same as MNC of 01)";
    reference
      "3GPP TS 23.003 subclause 2.2 and 12.1";
  }

  typedef Nci {
    type string;
    description
      "NR Cell Identity. The NCI shall be of fixed length of 36 bits
       and shall be coded using full hexadecimal representation.
       The exact coding of the NCI is the responsibility of each PLMN operator";
    reference
      "TS 23.003";
  }

  typedef OperationalState {
    type enumeration {
      enum DISABLED {
        value 0;
        description
          "The resource is totally inoperable.";
      }
      enum ENABLED {
        value 1;
        description
          "The resource is partially or fully operable.";
      }
    }
    description
      "TODO";
    reference
      "3GPP TS 28.625 and ITU-T X.731";
  }

  typedef AvailabilityStatus {
    type enumeration {
      enum IN_TEST {
        description
          "TODO";
      }
      enum FAILED {
        description
          "TODO";
      }
      enum POWER_OFF {
        description
          "TODO";
      }
      enum OFF_LINE {
        description
          "TODO";
      }
      enum OFF_DUTY {
        description
          "TODO";
      }
      enum DEPENDENCY {
        description
          "TODO";
      }
      enum DEGRADED {
        description
          "TODO";
      }
      enum NOT_INSTALLED {
        description
          "TODO";
      }
      enum LOG_FULL {
        description
          "TODO";
      }
    }
    description
      "TODO";
    reference
      "TODO";
  }

  typedef CellState {
    type enumeration {
      enum IDLE {
        description
          "TODO";
      }
      enum INACTIVE {
        description
          "TODO";
      }
      enum ACTIVE {
        description
          "TODO";
      }
    }
    description
      "TODO";
    reference
      "TODO";
  }

  typedef SNssai {
    type string;
    description
      "Single Network Slice Selection Assistance Information.";
    reference
      "TS 23.501 clause 5.15.2";
  }

  typedef Sst {
    type uint8;
    description
      "TODO";
    reference
      "TODO";
  }

  typedef Nrpci {
    type uint32;
    description
      "Physical Cell Identity (PCI) of the NR cell.";
    reference
      "TS 36.211 subclause 6.11";
  }

  typedef Tac {
    type int32 {
      range "0..16777215";
    }
    description
      "Tracking Area Code";
    reference
      "TS 23.003 clause 19.4.2.3";
  }

  typedef AmfRegionId {
    type string;
    description
      "";
    reference
      "clause 2.10.1 of 3GPP TS 23.003";
  }

  typedef AmfSetId {
    type string;
    description
      "";
    reference
      "clause 2.10.1 of 3GPP TS 23.003";
  }

  typedef AmfPointer {
    type string;
    description
      "";
    reference
      "clause 2.10.1 of 3GPP TS 23.003";
  }

  // type definitions especially for core NFs

  typedef NfType {
    type enumeration {
      enum NRF {
        description
          "TODO";
      }
      enum UDM {
        description
          "TODO";
      }
      enum AMF {
        description
          "TODO";
      }
      enum SMF {
        description
          "TODO";
      }
      enum AUSF {
        description
          "TODO";
      }
      enum NEF {
        description
          "TODO";
      }
      enum PCF {
        description
          "TODO";
      }
      enum SMSF {
        description
          "TODO";
      }
      enum NSSF {
        description
          "TODO";
      }
      enum UDR {
        description
          "TODO";
      }
      enum LMF {
        description
          "TODO";
      }
      enum GMLC {
        description
          "TODO";
      }
      enum 5G_EIR {
        description
          "TODO";
      }
      enum SEPP {
        description
          "TODO";
      }
      enum UPF {
        description
          "TODO";
      }
      enum N3IWF {
        description
          "TODO";
      }
      enum AF {
        description
          "TODO";
      }
      enum UDSF {
        description
          "TODO";
      }
      enum BSF {
        description
          "TODO";
      }
      enum CHF {
        description
          "TODO";
      }
    }
    description
      "TODO";
  }

  typedef NotificationType {
    type enumeration {
      enum N1_MESSAGES {
        description
          "TODO";
      }
      enum N2_INFORMATION {
        description
          "TODO";
      }
      enum LOCATION_NOTIFICATION {
        description
          "TODO";
      }
    }
    description
      "TODO";
  }

  typedef Load {
    type uint8 {
      range "0..100";
    }
    description
      "Latest known load information of the NF, percentage ";
  }

  typedef N1MessageClass {
    type enumeration {
      enum 5GMM {
        description
          "TODO";
      }
      enum SM {
        description
          "TODO";
      }
      enum LPP {
        description
          "TODO";
      }
      enum SMS {
        description
          "TODO";
      }
    }
    description
      "TODO";
  }

  typedef N2InformationClass {
    type enumeration {
      enum SM {
        description
          "TODO";
      }
      enum NRPPA {
        description
          "TODO";
      }
      enum PWS {
        description
          "TODO";
      }
      enum PWS_BCAL {
        description
          "TODO";
      }
      enum PWS_RF {
        description
          "TODO";
      }
    }
    description
      "TODO";
    reference
      "TODO";
  }

  typedef NsiId {
    type string;
    description
      "TODO";
  }

  typedef UeMobilityLevel {
    type enumeration {
      enum STATIONARY {
        description
          "TODO";
      }
      enum NOMADIC {
        description
          "TODO";
      }
      enum RESTRICTED_MOBILITY {
        description
          "TODO";
      }
      enum FULLY_MOBILITY {
        description
          "TODO";
      }
    }
    description
      "TODO";
    reference
      "TODO";
  }

  typedef ResourceSharingLevel {
    type enumeration {
      enum SHARED {
        description
          "TODO";
      }
      enum NOT_SHARED {
        description
          "TODO";
      }
    }
    description
      "TODO";
    reference
      "TODO";
  }

  typedef TxDirection {
    type enumeration {
      enum DL {
        description
          "TODO";
      }
      enum UL {
        description
          "TODO";
      }
      enum DL_AND_UL {
        description
          "TODO";
      }
    }
    description
      "TODO";
    reference
      "TODO";
  }

  typedef DistinguishedName { // TODO is this equivalent to TS 32.300 ?
    type string;
    description
      "Represents the international standard for the representation
       of Distinguished Name (RFC 4512).
       The format of the DistinguishedName REGEX is:
       {AttributeType = AttributeValue}

       AttributeType consists of alphanumeric and hyphen (OIDs not allowed).
       All other characters are restricted.
       The Attribute value cannot contain control characters or the
       following characters :  > < ; " + , (Comma) and White space
       The Attribute value can contain the following characters if they
       are excaped :  > < ; " + , (Comma) and White space
       The Attribute value can contain control characters if its an escaped
       double digit hex number.
       Examples could be
       UID=nobody@example.com,DC=example,DC=com
       CN=John Smith,OU=Sales,O=ACME Limited,L=Moab,ST=Utah,C=US";
    reference
      "RFC 4512 Lightweight Directory Access Protocol (LDAP):
             Directory Information Models";
  } // recheck regexp it doesn''''t handle posix [:cntrl:]

  typedef QOffsetRange {
    type int8;
    units "dB";
    description
      "TODO";
    reference
      "TODO";
  }

  typedef QuotaType {
    type enumeration {
      enum STRICT {
        description
          "TODO";
      }
      enum FLOAT {
        description
          "TODO";
      }
    }
    description
      "TODO";
  }

  typedef CyclicPrefix {
    type enumeration {
      enum NORMAL {
        description
          "TODO";
      }
      enum EXTENDED {
        description
          "TODO";
      }
    }
    description
      "TODO";
  }

  grouping PLMNInfo {
    description
      "The PLMNInfo data type define a S-NSSAI member in a specific PLMNId, and it have
       two attributes PLMNId and S-NSSAI (PLMNId, S-NSSAI). The PLMNId represents a data type that
       is comprised of mcc (mobile country code) and mnc (mobile network code), (See TS 23.003
       subclause 2.2 and 12.1) and S-NSSAI represents an data type, that is comprised of an SST
       (Slice/Service type) and an optional SD (Slice Differentiator) field, (See TS 23.003 [13]).";
    uses PLMNId;
    list sNSSAIList {
      key "sNssai";
      uses sNSSAIConfig;
      description "List of sNSSAIs";
    }
  }

  grouping ManagedNFProfile {
    description
      "Defines profile for managed NF";
    reference
      "3GPP TS 23.501";
    leaf idx {
      type uint32;
      description
        "TODO";
      reference
        "3GPP TS 23.501";
    }
    leaf nfInstanceID {
      type yang:uuid;
      config false;
      mandatory false;
      description
        "This parameter defines profile for managed NF.
         The format of the NF Instance ID shall be a
         Universally Unique Identifier (UUID) version 4,
         as described in IETF RFC 4122 ";
    }
    leaf-list nfType {
      type NfType;
      config false;
      min-elements 1;
      description
        "Type of the Network Function";
    }
    leaf hostAddr {
      type inet:host;
      mandatory false;
      description
        "Host address of a NF";
    }
    leaf authzInfo {
      type string;
      description
        "This parameter defines NF Specific Service authorization
         information. It shall include the NF type (s) and NF realms/origins
         allowed to consume NF Service(s) of NF Service Producer.";
      reference
        "See TS 23.501";
    }
    leaf location {
      type string;
      description
        "Information about the location of the NF instance
         (e.g. geographic location, data center) defined by operator";
      reference
        "TS 29.510";
    }
    leaf capacity {
      type uint16;
      mandatory false;
      description
        "This parameter defines static capacity information
         in the range of 0-65535, expressed as a weight relative to other
         NF instances of the same type; if capacity is also present in the
         nfServiceList parameters, those will have precedence over this value.";
      reference
        "TS 29.510";
    }
    leaf nFSrvGroupId {
      type string;
      description
        "This parameter defines identity of the group that is
         served by the NF instance.
         May be config false or true depending on the ManagedFunction.
         Config=true for Udrinfo. Config=false for UdmInfo and AusfInfo.
         Shall be present if ../nfType = UDM or AUSF or UDR. ";
      reference
        "TS 29.510";
    }
    leaf-list supportedDataSetIds {
      type enumeration {
        enum SUBSCRIPTION {
          description
            "TODO";
        }
        enum POLICY {
          description
            "TODO";
        }
        enum EXPOSURE {
          description
            "TODO";
        }
        enum APPLICATION {
          description
            "TODO";
        }
      }
      description
        "List of supported data sets in the UDR instance.
         May be present if ../nfType = UDR";
      reference
        "TS 29.510";
    }
    leaf-list smfServingAreas {
      type string;
      description
        "Defines the SMF service area(s) the UPF can serve.
         Shall be present if ../nfType = UPF";
      reference
        "TS 29.510";
    }
    leaf priority {
      type uint16;
      description
        "This parameter defines Priority (relative to other NFs
         of the same type) in the range of 0-65535, to be used for NF selection;
         lower values indicate a higher priority. If priority is also present
         in the nfServiceList parameters, those will have precedence over
         this value. Shall be present if ../nfType = AMF ";
      reference
        "TS 29.510";
    }
  }


  grouping PLMNId {
    description
      "TODO";
    reference
      "TS 23.658";
    leaf mcc {
      type Mcc;
      mandatory true;
      description
        "TODO";
    }
    leaf mnc {
      type Mnc;
      mandatory true;
      description
        "TODO";
    }
  }

  grouping AmfIdentifier {
    description
      "The AMFI is constructed from an AMF Region ID,
       an AMF Set ID and an AMF Pointer.
       The AMF Region ID identifies the region,
       the AMF Set ID uniquely identifies the AMF Set within the AMF Region, and
       the AMF Pointer uniquely identifies the AMF within the AMF Set. ";
    leaf amfRegionId {
      type AmfRegionId;
      description
        "TODO";
    }
    leaf amfSetId {
      type AmfSetId;
      description
        "TODO";
    }
    leaf amfPointer {
      type AmfPointer;
      description
        "TODO";
    }
  }

  grouping DefaultNotificationSubscription {
    description
      "TODO";
    leaf notificationType {
      type NotificationType;
      description
        "TODO";
    }
    leaf callbackUri {
      type inet:uri;
      description
        "TODO";
    }
    leaf n1MessageClass {
      type N1MessageClass;
      description
        "TODO";
    }
    leaf n2InformationClass {
      type N2InformationClass;
      description
        "TODO";
    }
  }

  grouping Ipv4AddressRange {
    description
      "TODO";
    leaf start {
      type inet:ipv4-address;
      description
        "TODO";
    }
    leaf end {
      type inet:ipv4-address;
      description
        "TODO";
    }
  }

  grouping Ipv6PrefixRange {
    description
      "TODO";
    leaf start {
      type inet:ipv6-prefix;
      description
        "TODO";
    }
    leaf end {
      type inet:ipv6-prefix;
      description
        "TODO";
    }
  }

  grouping AddressWithVlan {
    description
      "TODO";
    leaf ipAddress {
      type inet:ip-address;
      description
        "TODO";
    }
    leaf vlanId {
      type uint16;
      description
        "TODO";
    }
  }

  grouping ManagedElementGroup {
    description
      "Abstract class representing telecommunications resources.";
    leaf dnPrefix {
      type DistinguishedName;
      description
        "Provides naming context and splits the DN into a DN Prefix and Local DN";
    }
    leaf userLabel {
      type string;
      description
        "A user-friendly name of this object.";
    }
    leaf locationName {
      type string;
      config false;
      description
        "The physical location (e.g. an address) of an entity";
    }
    leaf-list managedBy {
      type DistinguishedName;
      config false;
      description
        "Relates to the role played by ManagementSystem";
    }
    leaf-list managedElementTypeList {
      type string;
      config false;
      min-elements 1;
      description
        "The type of functionality provided by the ManagedElement.
         It may represent one ME functionality or a combination of
         Two examples of allowed values are:
         -  NodeB;
         -  HLR, VLR.";
    }
  } // Managed Element grouping

  grouping NearRTRICGroup {
    description
      "Abstract class representing Near RT RIC.";
    leaf dnPrefix {
      type DistinguishedName;
      description
        "Provides naming context and splits the DN into a DN Prefix and Local DN";
    }
    leaf userLabel {
      type string;
      description
        "A user-friendly name of this object.";
    }
    leaf locationName {
      type string;
      config false;
      description
        "The physical location (e.g. an address) of an entity";
    }
    leaf gNBId {
          type int64 { range "0..4294967295"; }
          config false;
          description "Identifies a gNB within a PLMN. The gNB Identifier (gNB ID)
            is part of the NR Cell Identifier (NCI) of the gNB cells.";
          reference "gNB ID in 3GPP TS 38.300, Global gNB ID in 3GPP TS 38.413";
        }

  	list pLMNInfoList {
          uses PLMNInfo;
          key "mcc mnc";
          description "The PLMNInfoList is a list of PLMNInfo data type. It defines which PLMNs that can be served by the nearRTRIC.";
        }
    list RRMPolicyRatio {
      key id;
      leaf id {
        type string;
        description
          "Key leaf";
      }
      container attributes {
        uses RRMPolicyRatioGroup;
      }
      description " The RRMPolicyRatio IOC is one realization of a RRMPolicy_ IOC, see the
        inheritance in Figure 4.2.1.2-1. This RRM framework allows adding new policies, both
        standardized (like RRMPolicyRatio) or as vendor specific, by inheriting from the
        abstract RRMPolicy_ IOC. For details see subclause 4.3.36.";
    }

  } // Near RT RIC grouping



  	grouping Configuration{
  		leaf configParameter{
  			type string;
  			description "Type of the configuration parameter";
        }
  		leaf configValue{
  			type int64;
  			description "Identifies the configuration to be done for the network elements under the NearRTRIC";

  		}
  	}


  grouping GNBDUFunctionGroup {
    description
      "Represents the GNBDUFunction IOC.";
    reference
      "3GPP TS 28.541";

    leaf gNBId {
      type int64 {
        range "0..4294967295";
      }
      config false;
      mandatory false;
      description
        "Identifies a gNB within a PLMN. The gNB Identifier (gNB ID)
         is part of the NR Cell Identifier (NCI) of the gNB cells.";
      reference
        "gNB ID in 3GPP TS 38.300, Global gNB ID in 3GPP TS 38.413";
    }
    leaf gNBIdLength {
      type int32 {
        range "22..32";
      }
      mandatory false;
      description
        "Indicates the number of bits for encoding the gNB ID.";
      reference
        "gNB ID in 3GPP TS 38.300, Global gNB ID in 3GPP TS 38.413";
    }
    leaf gNBDUId {
      type int64 {
        range "0..68719476735";
      }
      mandatory false;
      description
        "Uniquely identifies the DU at least within a gNB.";
      reference
        "3GPP TS 38.473";
    }
    leaf gNBDUName {
      type string {
        length "1..150";
      }
      description
        "Identifies the Distributed Unit of an NR node";
      reference
        "3GPP TS 38.473";
    }
    list RRMPolicyRatio {
      key id;
      leaf id {
        type string;
        description
          "Key leaf";
      }
      container attributes {
        uses RRMPolicyRatioGroup;
      }
      description " The RRMPolicyRatio IOC is one realization of a RRMPolicy_ IOC, see the
        inheritance in Figure 4.2.1.2-1. This RRM framework allows adding new policies, both
        standardized (like RRMPolicyRatio) or as vendor specific, by inheriting from the
        abstract RRMPolicy_ IOC. For details see subclause 4.3.36.";
    }
  }

  grouping NRCellDUGroup {
    description
      "Represents the NRCellDU IOC.";
    reference
      "3GPP TS 28.541";
    list RRMPolicyRatio {
      key id;
      leaf id {
        type string;
        description
          "Key leaf";
      }
      container attributes {
        uses RRMPolicyRatioGroup;
      }
      description " The RRMPolicyRatio IOC is one realization of a RRMPolicy_ IOC, see the
        inheritance in Figure 4.2.1.2-1. This RRM framework allows adding new policies, both
        standardized (like RRMPolicyRatio) or as vendor specific, by inheriting from the
        abstract RRMPolicy_ IOC. For details see subclause 4.3.36.";
    }
    leaf cellLocalId {
      type int32 {
        range "0..16383";
      }
      mandatory false;
      description
        "Identifies an NR cell of a gNB. Together with the
         corresponding gNB identifier in forms the NR Cell Identity (NCI).";
      reference
        "NCI in 3GPP TS 38.300";
    }
    list pLMNInfoList {
      key "mcc mnc";
      min-elements 1;
      description
        "The PLMNInfoList is a list of PLMNInfo data type. It defines which PLMNs that
         can be served by the NR cell, and which S-NSSAIs that can be supported by the NR cell for
         corresponding PLMN in case of network slicing feature is supported. The plMNId of the first
         entry of the list is the PLMNId used to construct the nCGI for the NR cell.";
      uses PLMNInfo;
    }
    leaf nRPCI {
      type int32 {
        range "0..1007";
      }
      mandatory false;
      description
        "The Physical Cell Identity (PCI) of the NR cell.";
      reference
        "3GPP TS 36.211";
    }
    leaf nRTAC {
      type Tac;
      description
        "The common 5GS Tracking Area Code for the PLMNs.";
      reference
        "3GPP TS 23.003, 3GPP TS 38.473";
    }
  } // grouping

  grouping rRMPolicyMemberGroup {
    description
      "TODO";
    uses PLMNId;
    leaf sNSSAI {
      type SNssai;
      description
        "This data type represents an RRM Policy member that will be part of a
         rRMPolicyMemberList. A RRMPolicyMember is defined by its pLMNId and sNSSAI (S-NSSAI).
         The members in a rRMPolicyMemberList are assigned a specific amount of RRM resources
         based on settings in RRMPolicy.";
    }
  }

  grouping RRMPolicyRatioGroup {

    uses RRMPolicy_Group;    // Inherits RRMPolicy_

    leaf quotaType {
      type QuotaType;
      mandatory false;
      description "The type of the quota which allows to allocate resources as
        strictly usable for defined slice(s) (strict quota) or allows that
        resources to be used by other slice(s) when defined slice(s) do not
        need them (float quota).";
    }

    leaf rRMPolicyMaxRatio {
      type uint8;
      mandatory false;
      units percent;
      description "The RRM policy setting the maximum percentage of radio
        resources to be allocated to the corresponding S-NSSAI list. This
        quota can be strict or float quota. Strict quota means resources are
        not allowed for other sNSSAIs even when they are not used by the
        defined sNSSAIList. Float quota resources can be used by other sNSSAIs
        when the defined sNSSAIList do not need them. Value 0 indicates that
        there is no maximum limit.";
    }

    leaf rRMPolicyMinRatio {
      type uint8;
      mandatory false;
      units percent;
      description "The RRM policy setting the minimum percentage of radio
        resources to be allocated to the corresponding S-NSSAI list. This
        quota can be strict or float quota. Strict quota means resources are
        not allowed for other sNSSAIs even when they are not used by the
        defined sNSSAIList. Float quota resources can be used by other sNSSAIs
        when the defined sNSSAIList do not need them. Value 0 indicates that
        there is no minimum limit.";
    }
    leaf rRMPolicyDedicatedRatio {
      type uint8;
      units percent;
      description "Dedicated Ration.";
      }
    description "Represents the RRMPolicyRatio concrete IOC.";
    }


  grouping sNSSAIConfig{
	leaf sNssai {
        type string;
        description "s-NSSAI of a network slice.";
	   reference "3GPP TS 23.003";
      }
 	 leaf status {
        type string;
        description "status of s-NSSAI";
      }
	list configData{
		uses Configuration;
		key "configParameter";
		description "List of configurations to be done at the network elements";
	}
	}

  grouping RRMPolicy_Group {
    description
      "This IOC represents the properties of an abstract RRMPolicy. The RRMPolicy_ IOC
       needs to be subclassed to be instantiated. It defines two attributes apart from those
       inherited from Top IOC, the resourceType attribute defines type of resource (PRB, RRC
       connected users, DRB usage etc.) and the rRMPolicyMemberList attribute defines the
       RRMPolicyMember(s)that are subject to this policy. An RRM resource (defined in resourceType
       attribute) is located in NRCellDU, NRCellCU, GNBDUFunction, GNBCUCPFunction or in
       GNBCUUPFunction. The RRMPolicyRatio IOC is one realization of a RRMPolicy_ IOC, see the
       inheritance in TS 28.541 Figure 4.2.1.2-1. This RRM framework allows adding new policies,
       both standardized (like RRMPolicyRatio) or as vendor specific, by inheriting from the
       abstract RRMPolicy_ IOC.";
    leaf resourceType {
      type string;
      mandatory false;
      description
        "The resourceType attribute defines type of resource (PRB, RRC connected users,
         DRB usage etc.) that is subject to policy. Valid values are ''''PRB'''', ''''RRC'''' or ''''DRB''''";
    }
    list rRMPolicyMemberList {
      key "idx";
      leaf idx {
        type uint32;
        description
          "TODO";
      }
      description
        "It represents the list of RRMPolicyMember (s) that the managed object
         is supporting. A RRMPolicyMember <<dataType>> include the PLMNId <<dataType>>
         and S-NSSAI <<dataType>>.";
      uses rRMPolicyMemberGroup;
    }
  } // grouping

  grouping GNBCUUPFunctionGroup {
    description
      "Represents the GNBCUUPFunction IOC.";
    reference
      "3GPP TS 28.541";

    list RRMPolicyRatio {
      key id;
      leaf id {
        type string;
        description
          "Key leaf";
      }
      container attributes {
        uses RRMPolicyRatioGroup;
      }
      description " The RRMPolicyRatio IOC is one realization of a RRMPolicy_ IOC, see the
        inheritance in Figure 4.2.1.2-1. This RRM framework allows adding new policies, both
        standardized (like RRMPolicyRatio) or as vendor specific, by inheriting from the
        abstract RRMPolicy_ IOC. For details see subclause 4.3.36.";
    }
    leaf gNBCUUPId {
      type uint64 {
        range "0..68719476735";
      }
      config false;
      mandatory false;
      description
        "Identifies the gNB-CU-UP at least within a gNB-CU-CP";
      reference
        "''''gNB-CU-UP ID'''' in subclause 9.3.1.15 of 3GPP TS 38.463";
    }
    leaf gNBId {
      type int64 {
      range "0..4294967295";
    }
      mandatory false;
      description
        "Indicates the number of bits for encoding the gNB Id.";
      reference
        "gNB Id in 3GPP TS 38.300, Global gNB ID in 3GPP TS 38.413";
    }
    list pLMNInfoList {
      key "mcc mnc";
      description
        "The PLMNInfoList is a list of PLMNInfo data type. It defines which PLMNs that
         can be served by the GNBCUUPFunction and which S-NSSAIs can be supported by the
         GNBCUUPFunction for corresponding PLMN in case of network slicing feature is supported";
      uses PLMNInfo;
    }
  } // grouping

  grouping GNBCUCPFunctionGroup {
    description
      "Represents the GNBCUCPFunction IOC.";
    reference
      "3GPP TS 28.541";
    list RRMPolicyRatio {
      key id;
      leaf id {
        type string;
        description
          "Key leaf";
      }
      container attributes {
        uses RRMPolicyRatioGroup;
      }
      description " The RRMPolicyRatio IOC is one realization of a RRMPolicy_ IOC, see the
        inheritance in Figure 4.2.1.2-1. This RRM framework allows adding new policies, both
        standardized (like RRMPolicyRatio) or as vendor specific, by inheriting from the
        abstract RRMPolicy_ IOC. For details see subclause 4.3.36.";
    }
    leaf gNBId {
      type int64 {
        range "0..4294967295";
      }
      mandatory false;
      description
        "Identifies a gNB within a PLMN. The gNB Identifier (gNB ID)
         is part of the NR Cell Identifier (NCI) of the gNB cells.";
      reference
        "gNB ID in 3GPP TS 38.300, Global gNB ID in 3GPP TS 38.413";
    }
    leaf gNBIdLength {
      type int32 {
        range "22..32";
      }
      mandatory false;
      description
        "Indicates the number of bits for encoding the gNB ID.";
      reference
        "gNB ID in 3GPP TS 38.300, Global gNB ID in 3GPP TS 38.413";
    }
    leaf gNBCUName {
      type string {
        length "1..150";
      }
      mandatory false;
      description
        "Identifies the Central Unit of an gNB.";
      reference
        "3GPP TS 38.473";
    }
    list pLMNId {
      key "mcc mnc";
      min-elements 1;
      max-elements 1;
      description
        "The PLMN identifier to be used as part of the global RAN
         node identity.";
      uses PLMNId;
    }
  } // grouping

  grouping NRCellCUGroup {
    description
      "Represents the NRCellCU IOC.";
    reference
      "3GPP TS 28.541";
    leaf cellLocalId {
      type int32 {
        range "0..16383";
      }
      mandatory false;
      description
        "Identifies an NR cell of a gNB. Together with corresponding
         gNB ID it forms the NR Cell Identifier (NCI).";
    }
    list pLMNInfoList {
      key "mcc mnc";
      min-elements 1;
      description
        "The PLMNInfoList is a list of PLMNInfo data type. It defines which PLMNs
         that can be served by the NR cell, and which S-NSSAIs that can be supported by the
         NR cell for corresponding PLMN in case of network slicing feature is supported.";
      uses PLMNInfo;
      // Note: Whether the attribute pLMNId in the pLMNInfo can be writable depends on the implementation.
    }
    list RRMPolicyRatio {
      key id;
      leaf id {
        type string;
        description
          "Key leaf";
      }
      container attributes {
        uses RRMPolicyRatioGroup;
      }
      description " The RRMPolicyRatio IOC is one realization of a RRMPolicy_ IOC, see the
        inheritance in Figure 4.2.1.2-1. This RRM framework allows adding new policies, both
        standardized (like RRMPolicyRatio) or as vendor specific, by inheriting from the
        abstract RRMPolicy_ IOC. For details see subclause 4.3.36.";
    }
  } // grouping NRCellCUGroup

  grouping NRCellRelationGroup {
    description
      "Represents the NRCellRelation IOC.";
    reference
      "3GPP TS 28.541";
    leaf nRTCI {
      type uint64;
      description
        "Target NR Cell Identifier. It consists of NR Cell
         Identifier (NCI) and Physical Cell Identifier of the target NR cell
         (nRPCI).";
    '
where name = 'cps-ran-schema-model@2021-01-28.yang'
and checksum = 'a825c571c4a1d585a7f09a3716dedbfab1146abc4725b75a16f9ac89440bf46b';
