package io.bdrc.ldspdi.sparql.results;

/*******************************************************************************
 * Copyright (c) 2018 Buddhist Digital Resource Center (BDRC)
 * 
 * If this file is a derivation of another work the license header will appear below; 
 * otherwise, this work is licensed under the Apache License, Version 2.0 
 * (the "License"); you may not use this file except in compliance with the License.
 * 
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ResultPage {
    
    public int pageNumber;
    public int numberOfPages;
    public int pageSize;
    public int numResults;
    public long execTime;
    public int hash;
    public boolean isLastPage;
    public boolean isFirstPage;    
    public ResultPageLinks pLinks;
    public List<String> headers;
    public ArrayList<QuerySolutionItem> rows;
    

    public ResultPage(Results res,int pageNumber,HashMap<String,String> hm) throws JsonProcessingException{
        
        this.pageNumber=pageNumber;
        pageSize=res.getPageSize();
        numResults=res.getNumResults();
        execTime=res.getExecTime();
        hash=res.getHash();
        headers=res.getHeaders();
        numberOfPages=res.getNumberOfPages();
        int offset=(pageNumber-1)*pageSize;               
        rows=new ArrayList<>();
        ArrayList<QuerySolutionItem> allRows=res.getRows();
        if(pageNumber<=numberOfPages) {
            for (int x=(offset); x<(offset+pageSize);x++) {
                try {
                rows.add(allRows.get(x));
                }
                catch(Exception ex) {
                    //For building the last page
                    break;
                }
            }
        }
        if(pageNumber==1) {
            isFirstPage=true;
        }
        else {
            isFirstPage=false;
        }
        if(pageNumber==res.numberOfPages) {
            isLastPage=true;
        }else {
            isLastPage=false;
        }
        pLinks=new ResultPageLinks(this,hm);
    }

    public ArrayList<QuerySolutionItem> getRows() {
        return rows;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getNumResults() {
        return numResults;
    }

    public long getExecTime() {
        return execTime;
    }

    public int getHash() {
        return hash;
    }

    public boolean isLastPage() {
        return isLastPage;
    }
    
    public boolean isFirstPage() {
        return isFirstPage;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public int getNumberOfPages() {
        return numberOfPages;
    }

    public ResultPageLinks getpLinks() {
        return pLinks;
    }
}
