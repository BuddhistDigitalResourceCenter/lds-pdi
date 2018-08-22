package io.bdrc.auth.rdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import io.bdrc.ldspdi.service.ServiceConfig;
import io.bdrc.restapi.exceptions.RestException;

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

public class ModelUpdate extends TimerTask{
    
    long time;

    @Override
    public void run() {
        long time=1;
        Long obj=Long.valueOf(RdfAuthModel.getUpdated());
        if(obj!=null) {
            time=(long)obj;
        }
        HttpClient client=HttpClientBuilder.create().build();
        HttpGet get=new HttpGet(ServiceConfig.getProperty("authUpdatePath"));        
        long lastUpdate=1;
        try {
            HttpResponse resp=client.execute(get);
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            resp.getEntity().writeTo(baos);
            lastUpdate = Long.parseLong(baos.toString());
            if(lastUpdate > time) {                
                //do update
                RdfAuthModel.update(lastUpdate);
            }
        } catch (IOException | RestException e) {            
            e.printStackTrace();
        }
    }

}
