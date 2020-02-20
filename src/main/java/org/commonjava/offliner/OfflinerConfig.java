package org.commonjava.offliner;

import org.commonjava.offliner.cli.Options;

import java.io.File;

public class OfflinerConfig
{
    private File mavenSettingsXml;

    private String mavenTypeMapping;

    private int threads;

    private int connections;

    private String proxy;

    private OfflinerConfig( final File mavenSettingsXml, final String mavenTypeMapping, final int threads,
                           final int connections, final String proxy )
    {
        this.mavenSettingsXml = mavenSettingsXml;
        this.mavenTypeMapping = mavenTypeMapping;
        this.threads = threads;
        this.connections = connections;
        this.proxy = proxy;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public int getThreads()
    {
        return threads;
    }

    public int getConnections()
    {
        return connections;
    }

    public String getProxy()
    {
        return proxy;
    }

    public File getMavenSettingsXml()
    {
        return mavenSettingsXml;
    }

    public String getMavenTypeMapping()
    {
        return mavenTypeMapping;
    }

    public static class Builder
    {
        private File mavenSettingsXml;

        private String mavenTypeMapping;

        private int threads;

        private int connections;

        private String proxy;

        private Builder()
        {
        }

        public Builder withMavenSettingsXml( final File mavenSettingsXml )
        {
            this.mavenSettingsXml = mavenSettingsXml;
            return this;
        }

        public Builder withMavenTypeMapping( final String mavenTypeMapping )
        {
            this.mavenTypeMapping = mavenTypeMapping;
            return this;
        }

        public Builder withThreads( final int threads )
        {
            this.threads = threads;
            return this;
        }

        public Builder withConnections( final int connections )
        {
            this.connections = connections;
            return this;
        }

        public Builder withProxy( final String proxy )
        {
            this.proxy = proxy;
            return this;
        }

        public OfflinerConfig build()
        {
            return new OfflinerConfig( mavenSettingsXml, mavenTypeMapping, threads, connections, proxy );
        }

        public Builder fromOptions( final Options opts )
        {
            this.mavenSettingsXml = opts.getSettingsXml();
            this.mavenTypeMapping = opts.getTypeMapping();
            this.threads = opts.getThreads();
            this.connections = opts.getConnections();
            this.proxy = opts.getProxy();

            return this;
        }
    }
}
