ps -ef|grep TexaspokerInsuranceGameService|grep -v grep|awk '{print $2}'|while read pid  
        do  
           kill $pid  
        done  
