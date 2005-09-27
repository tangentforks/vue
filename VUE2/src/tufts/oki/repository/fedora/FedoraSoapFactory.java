/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/*
 * FedoraSoapFactory.java
 *
 * Created on September 22, 2003, 4:17 PM
 */

package tufts.oki.repository.fedora;

/**
 *
 * @author  akumar03
 */
import org.osid.repository.*;
import java.net.*;
import java.io.*;
import java.util.Vector;

import javax.xml.namespace.QName;

import fedora.server.types.gen.*;
import fedora.server.utilities.DateUtility;

//axis files
import org.apache.axis.encoding.ser.*;
import java.net.*;
import java.io.*;
import org.apache.axis.types.NonNegativeInteger;
import org.apache.axis.client.Service;
import org.apache.axis.client.Call;
import javax.xml.rpc.ServiceException;
import java.rmi.RemoteException ;

public class FedoraSoapFactory {
    // preferences for Fedora
    
    /** Creates a new instance of FedoraSoapFactory */
   
    public static  Vector getDisseminationRecords(String pid,org.osid.repository.RecordStructure recordStructure,Repository repository)   throws org.osid.repository.RepositoryException  {
        Call call;
        Vector disseminationList = new Vector();
        try {
            call = getCallMethods(repository);
            ObjectMethodsDef[] objMethods= (ObjectMethodsDef[]) call.invoke(new Object[] {pid} );
            if(objMethods == null)
                throw new org.osid.repository.RepositoryException("tufts.oki.repository.Asset():No Disseminations  returned");
            else {
                for(int i=0;i<objMethods.length;i++){
                    Record record = new Record(new PID(objMethods[i].getMethodName()),recordStructure);
                    record.createPart(((DisseminationRecordStructure)recordStructure).getBDEFPartStructure().getId(),objMethods[i].getBDefPID());
                    String disseminationURL = repository.getFedoraProperties().getProperty("url.fedora.get")+pid+"/"+objMethods[i].getBDefPID()+"/"+objMethods[i].getMethodName();
                    record.createPart(((DisseminationRecordStructure)recordStructure).getDisseminationURLPartStructure().getId(), disseminationURL);
                    disseminationList.add(record);
                }
            }
        } catch(Throwable t) {
			t.printStackTrace();
            throw new org.osid.repository.RepositoryException("FedoraSoapFactory.getDisseminators "+t.getMessage());
        }
        return disseminationList;
    }
    
 
    public static  AssetIterator search(Repository repository,SearchCriteria lSearchCriteria)  throws org.osid.repository.RepositoryException {
        String term = lSearchCriteria.getKeywords();
        String maxResults = lSearchCriteria.getMaxReturns();
        String searchOperation = lSearchCriteria.getSearchOperation();
        String token = lSearchCriteria.getToken();
        
        Call call;
        String fedoraApiUrl = repository.getFedoraProperties().getProperty("url.fedora.api");
        
        
        FieldSearchResult searchResults=new FieldSearchResult();
        NonNegativeInteger maxRes=new NonNegativeInteger(maxResults);
        
        FieldSearchResult methodDefs = null;
        
        String[] resField=new String[4];
        resField[0]="pid";
        resField[1]="title";
        resField[2]="description";
        resField[3]="cModel";
        try {
            call = getCallSearch(repository);
            call.setOperationName(new QName(fedoraApiUrl,searchOperation));
            FieldSearchQuery query=new FieldSearchQuery();
            query.setTerms(term);
            java.util.Vector resultObjects = new java.util.Vector();
            if(searchOperation == SearchCriteria.FIND_OBJECTS) {
                methodDefs =    (FieldSearchResult) call.invoke(new Object[] {resField,maxRes,query} );
                ListSession listSession = methodDefs.getListSession();
                if(listSession != null)
                    lSearchCriteria.setToken(listSession.getToken());
                else
                    lSearchCriteria.setToken(null);
                
            }else {
                if(lSearchCriteria.getToken() != null) {
                    methodDefs =    (FieldSearchResult) call.invoke(new Object[] {lSearchCriteria.getToken()} );
                    ListSession listSession = methodDefs.getListSession();
                    if(listSession != null)
                        lSearchCriteria.setToken(listSession.getToken());
                    else
                        lSearchCriteria.setToken(null);
                }
            }
            
            if (methodDefs != null &&  methodDefs.getResultList().length > 0){
                ObjectFields[] fields= methodDefs.getResultList();
                lSearchCriteria.setResults(fields.length);
                for(int i=0;i<fields.length;i++) {
                    String title = "No Title";
                    if(fields[i].getTitle() != null)
                        title = fields[i].getTitle()[0];
                    resultObjects.add(new Asset(repository,fields[i].getPid(),title,repository.getAssetType(fields[i].getCModel())));
                    
                    
                }
            } else {
                System.out.println("search returned no results");
            }
            
            
            return new AssetIterator(resultObjects) ;
        }catch(Throwable t) {
            t.printStackTrace();
            throw new org.osid.repository.RepositoryException("FedoraSoapFactory.search"+t.getMessage());
            
        }
    }
    
    public static org.osid.repository.AssetIterator advancedSearch(Repository repository,SearchCriteria lSearchCriteria)  throws org.osid.repository.RepositoryException {
        Condition cond[] = lSearchCriteria.getConditions();
        String maxResults = lSearchCriteria.getMaxReturns();
        
        Call call;
        FieldSearchResult searchResults=new FieldSearchResult();
        NonNegativeInteger maxRes=new NonNegativeInteger(maxResults);
        String[] resField=new String[4];
        resField[0]="pid";
        resField[1]="title";
        resField[2]="description";
        resField[3]="cModel";
        try {
            call = getCallAdvancedSearch(repository);
            FieldSearchQuery query=new FieldSearchQuery();
            //query.setTerms(term);
            query.setConditions(cond);
            java.util.Vector resultObjects = new java.util.Vector();
            FieldSearchResult methodDefs =    (FieldSearchResult) call.invoke(new Object[] {resField,maxRes,query} );
            if (methodDefs != null){
                ObjectFields[] fields= methodDefs.getResultList();
                lSearchCriteria.setResults(fields.length);
                for(int i=0;i<fields.length;i++) {
                    String title = "No Title";
                    if(fields[i].getTitle() != null)
                        title = fields[i].getTitle()[0];
                    resultObjects.add(new Asset(repository,fields[i].getPid(),title,repository.getAssetType(fields[i].getCModel())));
                }
            } else {
                System.out.println("search return no results");
            }
            return new AssetIterator(resultObjects) ;
        }catch(Throwable t) {
            throw new org.osid.repository.RepositoryException("FedoraSoapFactory.advancedSearch"+t.getMessage());
        }
    }
    
   
    private static  Call getCallMethods(Repository repository)  throws org.osid.repository.RepositoryException  {
        //creates the new service and call instance
        Call call;
        try {
            String fedoraTypeUrl = repository.getFedoraProperties().getProperty("url.fedora.type");
            String fedoraApiUrl = repository.getFedoraProperties().getProperty("url.fedora.api");
            Service service = new Service();
            call=(Call)service.createCall();
            call.setTargetEndpointAddress(new URL(repository.getFedoraProperties().getProperty("url.fedora.soap.access")));
            //specify what method to call on the server
            call.setOperationName(new QName(fedoraApiUrl,"GetObjectMethods"));
            //create namingspaces for user defined types
            QName qn1=new QName(fedoraTypeUrl, "ObjectMethodsDef");
            QName qn2=new QName(fedoraTypeUrl, "ObjectProfile");
            QName qn3=new QName(fedoraTypeUrl, "MethodParmDef");
            // Any Fedora-defined types required by the SOAP service must be registered
            // prior to invocation so the SOAP service knows the appropriate
            // serializer/deserializer to use for these types.
            call.registerTypeMapping(ObjectMethodsDef.class, qn1,new BeanSerializerFactory(ObjectMethodsDef.class, qn1),
            new BeanDeserializerFactory(ObjectMethodsDef.class, qn1));
            call.registerTypeMapping(ObjectProfile.class, qn2,new BeanSerializerFactory(ObjectProfile.class, qn2),
            new BeanDeserializerFactory(ObjectProfile.class, qn2));
            call.registerTypeMapping(MethodParmDef.class, qn3,new BeanSerializerFactory(MethodParmDef.class, qn3),
            new BeanDeserializerFactory(MethodParmDef.class, qn3));
        }catch (Exception ex) {
            throw new org.osid.repository.RepositoryException("FedoraSoapFactory.getCallMethods "+ex.getMessage());
        }
        return call;
    }
   
    private static Call getCallSearch(Repository repository)  throws org.osid.repository.RepositoryException {
        Call call;
        try {
            String fedoraTypeUrl = repository.getFedoraProperties().getProperty("url.fedora.type");
            String fedoraApiUrl = repository.getFedoraProperties().getProperty("url.fedora.api");
            Service service = new Service();
            call=(Call) service.createCall();
            System.out.println("FEDORA ACCESS URL = "+repository.getFedoraProperties().getProperty("url.fedora.soap.access"));
            call.setTargetEndpointAddress(new URL(repository.getFedoraProperties().getProperty("url.fedora.soap.access")));
            
            QName qn1 = new QName(fedoraTypeUrl, "ObjectFields");
            QName qn2 = new QName(fedoraTypeUrl, "FieldSearchQuery");
            QName qn3 = new QName(fedoraTypeUrl, "FieldSearchResult");
            QName qn4 = new QName(fedoraTypeUrl, "Condition");
            QName qn5=new QName(fedoraTypeUrl, "ComparisonOperator");
            QName qn6=new QName(fedoraTypeUrl, "ListSession");
            call.registerTypeMapping(ObjectFields.class, qn1,
            new BeanSerializerFactory(ObjectFields.class, qn1),
            new BeanDeserializerFactory(ObjectFields.class, qn1));
            call.registerTypeMapping(FieldSearchQuery.class, qn2,
            new BeanSerializerFactory(FieldSearchQuery.class, qn2),
            new BeanDeserializerFactory(FieldSearchQuery.class, qn2));
            call.registerTypeMapping(FieldSearchResult.class, qn3,
            new BeanSerializerFactory(FieldSearchResult.class, qn3),
            new BeanDeserializerFactory(FieldSearchResult.class, qn3));
            call.registerTypeMapping(Condition.class, qn4,
            new BeanSerializerFactory(Condition.class, qn4),
            new BeanDeserializerFactory(Condition.class, qn4));
            call.registerTypeMapping(ComparisonOperator.class, qn5,
            new BeanSerializerFactory(ComparisonOperator.class, qn5),
            new BeanDeserializerFactory(ComparisonOperator.class, qn5));
            call.registerTypeMapping(ListSession.class, qn6,
            new BeanSerializerFactory(ListSession.class, qn6),
            new BeanDeserializerFactory(ListSession.class, qn6));
            return call;
        }catch (Exception ex) {
            throw new org.osid.repository.RepositoryException("FedoraSoapFactory.getCallSearch "+ex);
        }
    }
    
    private static Call getCallAdvancedSearch(Repository repository)  throws org.osid.repository.RepositoryException {
        Call call;
        try {
            String fedoraTypeUrl = repository.getFedoraProperties().getProperty("url.fedora.type");
            String fedoraApiUrl = repository.getFedoraProperties().getProperty("url.fedora.api");
            Service service = new Service();
            call=(Call) service.createCall();
            call.setTargetEndpointAddress(new URL(repository.getFedoraProperties().getProperty("url.fedora.soap.access")));
            call.setOperationName(new QName(fedoraApiUrl,"findObjects"));
            QName qn1 = new QName(fedoraTypeUrl, "ObjectFields");
            QName qn2 = new QName(fedoraTypeUrl, "FieldSearchQuery");
            QName qn3 = new QName(fedoraTypeUrl, "FieldSearchResult");
            QName qn4 = new QName(fedoraTypeUrl, "Condition");
            QName qn5=new QName(fedoraTypeUrl, "ComparisonOperator");
            QName qn6=new QName(fedoraTypeUrl, "ListSession");
            call.registerTypeMapping(ObjectFields.class, qn1,
            new BeanSerializerFactory(ObjectFields.class, qn1),
            new BeanDeserializerFactory(ObjectFields.class, qn1));
            call.registerTypeMapping(FieldSearchQuery.class, qn2,
            new BeanSerializerFactory(FieldSearchQuery.class, qn2),
            new BeanDeserializerFactory(FieldSearchQuery.class, qn2));
            call.registerTypeMapping(FieldSearchResult.class, qn3,
            new BeanSerializerFactory(FieldSearchResult.class, qn3),
            new BeanDeserializerFactory(FieldSearchResult.class, qn3));
            call.registerTypeMapping(Condition.class, qn4,
            new EnumSerializerFactory(Condition.class, qn4),
            new EnumDeserializerFactory(Condition.class, qn4));
            call.registerTypeMapping(ComparisonOperator.class, qn5,
            new EnumSerializerFactory(ComparisonOperator.class, qn5),
            new EnumDeserializerFactory(ComparisonOperator.class, qn5));
            call.registerTypeMapping(ListSession.class, qn6,
            new BeanSerializerFactory(ListSession.class, qn6),
            new BeanDeserializerFactory(ListSession.class, qn6));
            return call;
        }catch (Exception ex) {
            throw new org.osid.repository.RepositoryException("FedoraSoapFactory.getCallSearch "+ex.getMessage());
        }
    }
    
}
