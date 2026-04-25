package com.hyltest.rag_practice.utils;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import com.aliyun.oss.common.comm.SignVersion;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component  //声明为Spring Bean，将其交给Spring容器管理，需要时直接注入即可（解耦）
//作为第三方bean，一般不能直接加载到Spring容器中，要使用@Bean注解创建
public class AliyunOSSOperator {

    //想存储的bucket的基本信息：在对应bucket的概览中可以查看
    //这块信息是可能会变动的，所以这种硬编码的形式不适合，需要将其放到application.yml文件中，需要则进行读取
    //springboot提供了@Value注解，用于读取application.yml文件中的属性值，并注入到变量中
    private final AliyunOSSProperties aliyunOSSProperties;

    /*
    当一个字段被声明为 final 时，它必须在对象创建时被初始化，并且之后不能再被修改。
    因为要求在对象创建时被初始化，因此这只能通过以下方式实现：
        在声明时直接初始化
        在构造函数中初始化
     */
    public AliyunOSSOperator(AliyunOSSProperties aliyunOSSProperties) {
        this.aliyunOSSProperties = aliyunOSSProperties;
    }

    /**
     * 上传文件的方法
     * @param content 文件内容的字节数组
     * @param originalFilename 原始文件名->用于获得文件的后缀名，构建新的文件名
     * @return 文件访问路径
     */
    public String upload(byte[] content, String originalFilename) throws Exception {
        //不能写到方法外，否则可能因为注入时机不对而引发空指针异常
        String endpoint=aliyunOSSProperties.getEndpoint();
        String bucketName=aliyunOSSProperties.getBucketName();
        String region=aliyunOSSProperties.getRegion();

        // 1、从环境变量中获取访问凭证。运行本代码示例之前，请确保已设置环境变量OSS_ACCESS_KEY_ID和OSS_ACCESS_KEY_SECRET。
        EnvironmentVariableCredentialsProvider credentialsProvider = CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();

        // 2、生成一个文件名（根据日期进行分目录管理）
        //填写Object完整路径，例如2024/06/1.png。Object完整路径中不能包含Bucket名称。
        //获取当前系统日期的字符串,格式为 yyyy/MM -> 根据日期来进行文件的分类存储
        String dir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        //生成一个新的不重复的文件名
        String newFileName = UUID.randomUUID() + originalFilename.substring(originalFilename.lastIndexOf("."));
        String objectName = dir + "/" + newFileName;

        // 3、创建OSSClient实例。
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
        OSS ossClient = OSSClientBuilder.create()
                .endpoint(endpoint)
                .credentialsProvider(credentialsProvider)
                .clientConfiguration(clientBuilderConfiguration)
                .region(region)
                .build();

        try {
            // 4、上传文件到OSS对应的bucket中
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(content));
        } finally {
            ossClient.shutdown();
        }

        // 5、返回文件访问路径
        return endpoint.split("//")[0] + "//" + bucketName + "." + endpoint.split("//")[1] + "/" + objectName;
    }

}
