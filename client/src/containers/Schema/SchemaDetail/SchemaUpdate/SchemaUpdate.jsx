import React from 'react';
import Form from '../../../../components/Form/Form';
import Joi from 'joi-browser';
import { uriLatestSchemaVersion, uriUpdateSchema } from '../../../../utils/endpoints';
import { toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

class SchemaUpdate extends Form {
  state = {
    clusterId: '',
    schemaId: '',
    compatibilityOptions: [
      {
        _id: 'NONE',
        name: 'NONE'
      },
      {
        _id: 'BACKWARD',
        name: 'BACKWARD'
      },
      {
        _id: 'BACKWARD_TRANSITIVE',
        name: 'BACKWARD_TRANSITIVE'
      },
      {
        _id: 'FORWARD',
        name: 'FORWARD'
      },
      {
        _id: 'FORWARD_TRANSITIVE',
        name: 'FORWARD_TRANSITIVE'
      },
      {
        _id: 'FULL',
        name: 'FULL'
      },
      {
        _id: 'FULL_TRANSITIVE',
        name: 'FULL_TRANSITIVE'
      }
    ],
    formData: {
      subject: '',
      compatibility: '',
      schema: ''
    },
    errors: {},
    roles: JSON.parse(sessionStorage.getItem('roles'))
  };

  schema = {
    subject: Joi.string()
        .required()
        .label('Subject'),
    compatibility: Joi.string()
        .required()
        .label('Compatibility Level'),
    schema: Joi.string()
        .required()
        .label('Latest Schema')
  };

  componentDidMount() {
    const { clusterId } = this.props.match.params;
    const { schemaId } = this.props || this.props.match.params;
    this.setState({ clusterId, schemaId }, () => this.getLatestSchemaVersion(clusterId, schemaId));
  }

  async getLatestSchemaVersion() {
    const { clusterId, schemaId } = this.state;
    let data = await this.getApi(uriLatestSchemaVersion(clusterId, schemaId));

    data = data.data;
    if (data) {
      this.handleLatestSchemaVersion(data);
    }
  }

  handleLatestSchemaVersion = latestSchemaVersion => {
    const { formData } = { ...this.state };

    formData.subject = latestSchemaVersion.subject;
    formData.compatibility = latestSchemaVersion.compatibilityLevel;
    formData.schema = JSON.stringify(JSON.parse(latestSchemaVersion.schema), null, 2);

    this.setState(formData);
  };

  doSubmit() {
    const { clusterId, formData } = this.state;
    const body = {
      clusterId,
      subject: formData.subject,
      compatibilityLevel: formData.compatibility,
      schema: formData.schema
    };

    this.postApi(uriUpdateSchema(clusterId, formData.subject), body)
        .then(() => {
          toast.success(`Schema '${formData.subject}' is updated`);
          this.props.getSchemaVersions();
          window.location.reload(false);
        });
  }

  render() {
    const { compatibilityOptions, roles } = this.state;

    return (
        <form
            encType="multipart/form-data"
            className="khq-form khq-form-config"
            onSubmit={e => this.handleSubmit(e)}
        >
          <fieldset>
            {this.renderInput(
                'subject',
                'Subject',
                'Subject',
                undefined,
                undefined,
                false,
                false,
                false,
                { disabled: true }
            )}
            {this.renderSelect(
                'compatibility',
                'Compatibility Level',
                compatibilityOptions,
                ({ currentTarget: input }) => {
                  let { formData } = { ...this.state };
                  formData.compatibility = input.value;
                  this.setState({ formData });
                },
                'col-sm-10'
            )}
            {this.renderJSONInput('schema', 'Latest Schema', value => {
                  let { formData } = { ...this.state };
                  formData.schema = value;
                  this.setState({ formData });
                },
                'col-sm-10'
            )}
            {roles.registry['registry/update'] && (
                this.renderButton('Update', undefined, undefined, 'submit'))}
          </fieldset>
        </form>
    );
  }
}

export default SchemaUpdate;
