package io.github.athingx.athing.aliyun.tunnel.agent;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import io.github.athingx.athing.aliyun.tunnel.core.Tunnel;
import io.github.athingx.athing.aliyun.tunnel.core.TunnelConfig;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Scanner;

/**
 * 启动器
 */
public class AppLauncher {

    private boolean isHelp;
    private File tunCfgFile = new File(String.format(".%scfg%sconfig.json", File.separatorChar, File.separatorChar));
    private File logCfgFile = new File(String.format(".%scfg%slogback.xml", File.separatorChar, File.separatorChar));

    private TunnelConfig config;

    private AppLauncher(String[] arguments) {

        try {

            // 解析参数
            parseOpt(arguments);

            // 如果只是输出帮助，则直接退出即可
            if (isHelp) {
                printHelp();
                System.exit(0);
            }

            // 加载logback配置文件
            configLogback(logCfgFile);

            // 加载tunnel配置文件
            configTunnel(tunCfgFile);

            // 启动隧道
            launch();


        } catch (ExitException cause) {
            System.err.printf("%s;%s%n", cause.getCode(), cause.getMessage());
            System.exit(cause.getCode());
        } catch (Exception cause) {
            System.err.printf("%s;%s%n", ExitException.ERR_UN_KNOW, cause.getMessage());
        }

    }

    /**
     * 解析参数
     *
     * @param arguments 参数
     */
    private void parseOpt(String[] arguments) {
        final Getopt opt = new Getopt("tunnel", arguments, "hC:L:", new LongOpt[]{
                new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'),
                new LongOpt("config", LongOpt.OPTIONAL_ARGUMENT, null, 'C'),
                new LongOpt("logback", LongOpt.OPTIONAL_ARGUMENT, null, 'L')
        });
        opt.setOpterr(false);

        for (int ch; (ch = opt.getopt()) != -1; ) {
            switch (ch) {
                case 'h': {
                    isHelp = true;
                    break;
                }
                case 'C': {
                    tunCfgFile = new File(opt.getOptarg());
                    break;
                }
                case 'L': {
                    logCfgFile = new File(opt.getOptarg());
                    break;
                }
                case '?': {
                    throw new ExitException(ExitException.ERR_ARGUMENTS_PARSE_FAIL, "illegal arguments!");
                }
            }
        }
    }


    /**
     * 输出帮助信息
     */
    private void printHelp() {
        try (final Scanner scanner = new Scanner(Objects.requireNonNull(getClass().getResourceAsStream("/io/github/athingx/athing/aliyun/tunnel/agent/help.txt")))) {
            while (scanner.hasNextLine()) {
                System.out.println(scanner.nextLine());
            }
        } catch (Exception cause) {
            throw new ExitException(ExitException.ERR_UN_KNOW, cause);
        }
    }

    /**
     * 配置logback
     *
     * @param file 配置文件
     */
    private void configLogback(File file) {

        if (!file.exists() || !file.canRead()) {
            throw new ExitException(
                    ExitException.ERR_LOGBACK_NOT_EXISTED,
                    String.format("logback config read fail: %s", file)
            );
        }

        try (final InputStream input = new FileInputStream(file)) {
            final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            final JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            context.reset();
            configurator.doConfigure(input);
        } catch (Exception cause) {
            throw new ExitException(ExitException.ERR_LOGBACK_PARSE_FAIL, cause);
        }

    }

    /**
     * 启动隧道
     */
    private void launch() {

        try {
            final String name = String.format("/%s/%s/tunnel", config.getAccess().getProductId(), config.getAccess().getThingId());
            final Tunnel tunnel = new Tunnel(name, config);
            Runtime.getRuntime().addShutdownHook(new Thread(tunnel::destroy, "tunnel-shutdown-hook"));
            tunnel.connect();
            System.out.printf("tunnel is connect to: %s%n", config.getConnect().getRemote());
        } catch (URISyntaxException cause) {
            throw new ExitException(ExitException.ERR_CONFIG_PARSE_FAIL, cause.getMessage());
        }

    }

    private void configTunnel(File file) {

        if (!file.exists() || !file.canRead()) {
            throw new ExitException(
                    ExitException.ERR_CONFIG_NOT_EXISTED,
                    String.format("tunnel config read fail: %s", file)
            );
        }

        try (final Reader reader = new FileReader(file)) {
            config = new GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
                    .create()
                    .fromJson(reader, TunnelConfig.class);
        } catch (Exception cause) {
            throw new ExitException(ExitException.ERR_CONFIG_PARSE_FAIL, cause);
        }

    }

    private static class ExitException extends RuntimeException {

        public static final int ERR_UN_KNOW = -1;
        public static final int ERR_ARGUMENTS_PARSE_FAIL = -2;
        public static final int ERR_CONFIG_NOT_EXISTED = -101;
        public static final int ERR_CONFIG_PARSE_FAIL = -102;
        public static final int ERR_LOGBACK_NOT_EXISTED = -201;
        public static final int ERR_LOGBACK_PARSE_FAIL = -202;


        private final int code;

        ExitException(int code, Throwable cause) {
            super(cause);
            this.code = code;
        }

        ExitException(int code, String message) {
            super(message);
            this.code = code;
        }

        public int getCode() {
            return code;
        }

    }

    /**
     * 启动
     *
     * @param args 启动参数
     */
    public static void main(String... args) {
        new AppLauncher(args);
    }

}
