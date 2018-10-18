package io.bdrc.ldspdi.service;

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

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.ldspdi.sparql.QueryConstants;



public class GitService implements Runnable{

    private static String GIT_LOCAL_PATH;
    private static String GIT_REMOTE_URL="https://github.com/BuddhistDigitalResourceCenter/lds-queries.git";
    private static Repository localRepo;


    final static Logger log = LoggerFactory.getLogger(GitService.class.getName());

    public static void update(String localPath) {

        GIT_LOCAL_PATH=localPath;
        FileRepositoryBuilder builder=new FileRepositoryBuilder();
        File localGit=new File(GitService.GIT_LOCAL_PATH+"/.git");
        File WlocalGit=new File(GitService.GIT_LOCAL_PATH);
        boolean isGitRepo=RepositoryCache.FileKey.isGitRepository(localGit, FS.DETECTED);

        //init local git dir and clone remote repository if not present locally
        if(!isGitRepo) {
            initRepo();
        }
        else {
            try {
                localRepo = builder.setGitDir(localGit)
                        .setWorkTree(WlocalGit)
                        .readEnvironment() // scan environment GIT_* variables
                        .build();

            }
            catch(IOException ex) {
                log.error("Git was unable to setup repository at "+localGit.getPath()+" directory ", ex.getMessage());

            }
            updateRepo();
        }

    }

    private static void initRepo() {
        try {

            Git result = Git.cloneRepository()
                    .setDirectory(new File(GitService.GIT_LOCAL_PATH))
                    .setURI(GitService.GIT_REMOTE_URL)
                    .setProgressMonitor(new TextProgressMonitor()).call();
            result.checkout().setName("master").call();
            result.close();

        }
        catch(Exception ex) {
            log.error(" Git was unable to pull repository : "+GitService.GIT_REMOTE_URL+" directory ", ex.getMessage());

        }
    }

    private static void updateRepo() {
        try {

            Git git=new Git(localRepo);
            git.pull().setProgressMonitor(new TextProgressMonitor()).call();
            git.close();

        }
        catch(Exception ex) {
            log.error(" Git was unable to pull repository : "+GitService.GIT_REMOTE_URL+" directory "+ex.getMessage());

        }
    }

    @Override
    public void run() {
        update(ServiceConfig.getProperty(QueryConstants.QUERY_PATH));
    }


}
